package com.wrdsbackend.tenkbackendservice.service;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.wrdsbackend.tenkbackendservice.config.OzoneConfig;
import com.wrdsbackend.tenkbackendservice.config.TenkAppConfig;
import com.wrdsbackend.tenkbackendservice.dao.TenkDao;
import com.wrdsbackend.tenkbackendservice.dto.*;

import com.wrdsbackend.tenkbackendservice.dto.internal.FilingItemInternalRespDto;
import com.wrdsbackend.tenkbackendservice.dto.internal.FilingItemizationInternalReqDto;
import com.wrdsbackend.tenkbackendservice.dto.internal.ItemizationResultAggregator;
import com.wrdsbackend.tenkbackendservice.dto.internal.StoredItemNameSuffix;
import com.wrdsbackend.tenkbackendservice.error.FilingProcessException;
import com.wrdsbackend.tenkbackendservice.error.UnableToGetFilingResourceException;
import com.wrdsbackend.tenkbackendservice.error.UnexpectedInternalError;
import com.wrdsbackend.tenkbackendservice.util.TenkFilingDataUtil;
import com.wrdsbackend.tenkbackendservice.util.TenkURLUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class TenkService {

    private final TenkAppConfig tenkAppConfig;
    private final TenkDao tenkDao;
    private final TenkFilingDataUtil tenkFilingDataUtil;
    private final TenkItemizationService itemizationService;
    private final OzoneConfig ozoneConfig;
    private final S3Client s3Client;

    public Mono<ShortResourceUrlsDto> processFilingURL(String url) {
        String filingName = TenkURLUtil.extractFilingNameFromFilingTxtURL(url);
        log.info("Extracted filing name: '{}' from url '{}'.", filingName, url);

        StoredItemNameSuffix typeSuffix = tenkAppConfig.getStoredItemNameSuffix();

        try {
            // Try if all three
            for (String fileSuffix : new String[]{typeSuffix.getJson(), typeSuffix.getHtml(), typeSuffix.getText()}) {
                s3Client.headObject(
                        HeadObjectRequest.builder()
                                .bucket(ozoneConfig.getBucketName())
                                .key(filingName + fileSuffix + ".zip")
                                .build());
            }
            log.info("All resources are already present in Ozone, no need to download and process the filing.");
            return Mono.just(new ShortResourceUrlsDto(
                    filingName + typeSuffix.getJson(),
                    filingName + typeSuffix.getHtml(),
                    filingName + typeSuffix.getText()));
        } catch (NoSuchKeyException e) {
            log.info("One of the json/html/text file is not stored in Ozone yet, Got response status '{}' along with the message: '{}'",
                    e.statusCode(), e.getMessage());
            log.info("Preparing to download and process the filing content.");
        } catch (S3Exception e) {
            log.error("Got unexpected status when talking to Ozone. Got response status '{}' along with the message: '{}'",
                    e.statusCode(), e.getMessage());
            throw new UnexpectedInternalError();
        }

        String zipFileSuffix = ".zip";

        String htmlFilingObjKey = filingName + typeSuffix.getHtml() + zipFileSuffix;
        String textFilingObjKey = filingName + typeSuffix.getText() + zipFileSuffix;
        String jsonFilingObjKey = filingName + typeSuffix.getJson() + zipFileSuffix;

        int dataBufInitSize = 4096;
        ByteArrayOutputStream jsonBos = new ByteArrayOutputStream(dataBufInitSize);
        ByteArrayOutputStream htmlBos = new ByteArrayOutputStream(dataBufInitSize);
        ByteArrayOutputStream textBos = new ByteArrayOutputStream(dataBufInitSize);

        ItemizationResultAggregator itemizationResultAggregator = new ItemizationResultAggregator(
                new ZipOutputStream(htmlBos),
                new ZipOutputStream(textBos),
                new ArrayList<>()
        );

        return itemizationService.getFilingItems(new FilingItemizationInternalReqDto(url))
                .publishOn(Schedulers.boundedElastic())
                // This would gather the itemized part into a ItemizationResultAggregator structure, This helps us reduce the request load on DB and object store.
                .reduce(itemizationResultAggregator, (aggregator, fetchedItem) ->
                        aggregateItemizedItem(filingName, typeSuffix, aggregator, fetchedItem))
                // Below are IO heavy tasks, do these asynchronously.
                .<ItemizationResultAggregator>handle((aggregator, sink) -> {
                    try {
                        log.info("Persisting items to DB.");
                        // persist items to hbase
                        List<String> composedRowKeys = tenkDao.persist(aggregator.tenkFilingDbItems());
                        log.info("persisted keys: {}", composedRowKeys);

                        sink.next(aggregator);
                    } catch (IOException e) {
                        log.error("Failed to persist filing item '{}' to DB. Reason: '{}'", filingName, e.getMessage());
                        sink.error(new FilingProcessException(filingName));
                    }
                })
                .<ItemizationResultAggregator>handle((aggregator, sink) -> {
                    try {
                        log.info("Persisting itemized forms as json file to Object Store.");
                        List<FilingItem> filingItems = aggregator.tenkFilingDbItems().stream()
                                .map(dbItem -> new FilingItem(dbItem.getItemNumber(), dbItem.getHtmlContent()))
                                .toList();
                        // write all filing items in a zipped JSON file.
                        ZipOutputStream jsonZipOutputStream = new ZipOutputStream(jsonBos);
                        jsonZipOutputStream.putNextEntry(new ZipEntry(filingName + ".json"));
                        jsonZipOutputStream.write(new ObjectMapper().writeValueAsBytes(filingItems));
                        jsonZipOutputStream.closeEntry();
                        jsonZipOutputStream.close();
                        persistDataToOzone(jsonBos.toByteArray(), jsonFilingObjKey);

                        sink.next(aggregator);
                    } catch (IOException e) {
                        log.error("Failed to process file '{}' when uploading it to ozone. Reason: '{}'", jsonFilingObjKey, e.getMessage());
                        sink.error(new FilingProcessException(filingName));
                    }
                })
                .<ItemizationResultAggregator>handle((aggregator, sink) -> {
                    try {
                        log.info("Persisting html zip to Object Store.");
                        // persist html zip to ozone
                        aggregator.htmlOutputStream().close();
                        persistDataToOzone(htmlBos.toByteArray(), htmlFilingObjKey);

                        sink.next(aggregator);
                    } catch (IOException e) {
                        log.error("Failed to process file '{}' when uploading it to ozone. Reason: '{}'", htmlFilingObjKey, e.getMessage());
                        sink.error(new FilingProcessException(filingName));
                    }
                })
                .<ItemizationResultAggregator>handle((aggregator, sink) -> {
                    log.info("Persisting text zip to Object Store.");
                    try {
                        // persist text zip to ozone
                        aggregator.textOutputStream().close();
                        persistDataToOzone(textBos.toByteArray(), textFilingObjKey);

                        sink.next(aggregator);
                    } catch (IOException e) {
                        log.error("Failed to process file '{}' when uploading it to ozone. Reason: '{}'", textFilingObjKey, e.getMessage());
                        sink.error(new FilingProcessException(filingName));
                    }
                })
                .then(Mono.just(new ShortResourceUrlsDto(
                        filingName + typeSuffix.getJson(),
                        filingName + typeSuffix.getHtml(),
                        filingName + typeSuffix.getText())))
                .onErrorResume(error -> {
                    if (error instanceof FilingProcessException fpe) {
                        return Mono.error(fpe);
                    }
                    else if (error instanceof WebClientResponseException wce) {
                        log.error("Unable to get Filing Resource from the given URL : '{}'. " +
                                        "Got: '{}' status from the itemization service, along with the error message: '{}', and response header {}",
                                url, wce.getStatusCode(), wce.getResponseBodyAsString(), wce.getHeaders());
                        return Mono.error(new UnableToGetFilingResourceException(url, wce.getStatusCode()));
                    }
                    return Mono.error(new UnexpectedInternalError());
                });
    }

    private void persistDataToOzone(byte[] data, String objectKeyName) {
        log.info("Uploading object '{}' to Ozone.", objectKeyName);

        PutObjectResponse putObjectResult = s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(ozoneConfig.getBucketName())
                        .key(objectKeyName)
                        .contentLength((long) data.length)
                        .build(),
                RequestBody.fromBytes(data));

        log.info("Successfully upload file '{}' to Ozone; bucketKeyEnabled '{}'; Status '{}'; Response Headers '{}'",
                objectKeyName, putObjectResult.bucketKeyEnabled(), putObjectResult.sdkHttpResponse().statusCode(), putObjectResult.sdkHttpResponse().headers());
    }

    private ItemizationResultAggregator aggregateItemizedItem(String filingName, StoredItemNameSuffix typeSuffix, ItemizationResultAggregator aggregator, FilingItemInternalRespDto fetchedItem) {
        try {
            log.info("processing {}...", fetchedItem.getItemName());
            String itemHtmlContent = fetchedItem.getItemHtmlContent();
            String itemTextContent = fetchedItem.getItemTextContent();

            // store html content as a zip entry
            ZipEntry htmlZipEntry = new ZipEntry(filingName + fetchedItem.getItemName() + typeSuffix.getHtml());
            aggregator.htmlOutputStream().putNextEntry(htmlZipEntry);
            aggregator.htmlOutputStream().write(itemHtmlContent.getBytes(StandardCharsets.UTF_8));
            aggregator.htmlOutputStream().closeEntry();

            // store text content as a zip entry
            ZipEntry textZipEntry = new ZipEntry(filingName + fetchedItem.getItemName() + typeSuffix.getText());
            aggregator.textOutputStream().putNextEntry(textZipEntry);
            aggregator.textOutputStream().write(itemTextContent.getBytes(StandardCharsets.UTF_8));
            aggregator.textOutputStream().closeEntry();

            // store to db item buffer
            aggregator.tenkFilingDbItems().add(tenkFilingDataUtil.convertTenkFilingDtoToTenkFilingDbItem(filingName, fetchedItem));
            return aggregator;
        } catch (IOException e) {
            log.error("Filed to handle zip stream when processing filing '{}'. Reason: '{}'.", filingName, e.getMessage());
            throw new FilingProcessException(filingName);
        }
    }

    public Mono<InputStreamResource> getItemizedFiling(String filingName) {
        String objKey = filingName + ".zip";

        try {
            return Mono.just(new InputStreamResource(s3Client.getObject(
                    GetObjectRequest.builder()
                            .bucket(ozoneConfig.getBucketName())
                            .key(objKey)
                            .build())));
        } catch (NoSuchKeyException e) {
            log.info("Filing resource '{}' does not exists, Got response status '{}' along with the message: '{}'",
                    objKey, e.statusCode(), e.getMessage());
            throw new UnableToGetFilingResourceException(filingName, HttpStatusCode.valueOf(e.statusCode()));
        } catch (S3Exception e) {
            log.error("Got unexpected status when talking to Ozone. Got response status '{}' along with the message: '{}'",
                    e.statusCode(), e.getMessage());
            throw new UnexpectedInternalError();
        }
    }
}
