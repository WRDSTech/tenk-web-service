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
import com.wrdsbackend.tenkbackendservice.error.UnableToPersistItemToDbException;
import com.wrdsbackend.tenkbackendservice.error.UnableToPutObjectToStoreException;
import com.wrdsbackend.tenkbackendservice.util.TenkFilingDataUtil;
import com.wrdsbackend.tenkbackendservice.util.TenkURLUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.io.FileUtils;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
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

    private final String tempFilingDir = "temp-filing/";

    public Mono<ShortResourceUrlsDto> processFilingURL(String url) {
        String filingName = TenkURLUtil.extractFilingNameFromFilingTxtURL(url);
        log.info("Extracted filing name: '{}' from url '{}'.", filingName, url);

        // TODO: Test if any of the json/text/html file not in our storage. If yes proceed and process the url.


        try {
            StoredItemNameSuffix typeSuffix = tenkAppConfig.getStoredItemNameSuffix();
            String tempHtmlZipFileName = tempFilingDir + filingName + typeSuffix.getHtml() + ".zip";
            String tempTextZipFileName = tempFilingDir + filingName + typeSuffix.getText() + ".zip";
            String tempJsonFileName = tempFilingDir + filingName + typeSuffix.getJson() + ".json";

            ItemizationResultAggregator itemizationResultAggregator = new ItemizationResultAggregator(
                    new ZipOutputStream(new FileOutputStream(tempHtmlZipFileName)),
                    new ZipOutputStream(new FileOutputStream(tempTextZipFileName)),
                    new ArrayList<>()
            );

            // TODO: Properly Handle the first getFilingItems RPC error. Should have a 404 for non-existing resource, and 500 for others.

            itemizationService.getFilingItems(new FilingItemizationInternalReqDto(url))
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
                            sink.error(new UnableToPersistItemToDbException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage()));
                        }
                    })
                    .<ItemizationResultAggregator>handle((aggregator, sink) -> {
                        try {
                            log.info("Persisting itemized forms as json file to Object Store.");
                            List<FilingItem> filingItems = aggregator.tenkFilingDbItems().stream()
                                    .map(dbItem -> new FilingItem(dbItem.getItemNumber(), dbItem.getHtmlContent()))
                                    .toList();

                            FileOutputStream jsonFileOutputStream = new FileOutputStream(tempJsonFileName);

                            jsonFileOutputStream.write(new ObjectMapper().writeValueAsBytes(filingItems));
                            jsonFileOutputStream.flush();
                            jsonFileOutputStream.close();
                            
                            // persist the temp json file to ozone
                            persistFileToOzone(tempJsonFileName);

                            sink.next(aggregator);
                        } catch (IOException e) {
                            log.error("Failed to process file '{}' when uploading to ozone. Reason: '{}'", tempJsonFileName, e.getMessage());
                            sink.error(new UnableToPutObjectToStoreException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage()));
                        }
                    })
                    .<ItemizationResultAggregator>handle((aggregator, sink) -> {
                        try {
                            log.info("Persisting html zip to Object Store.");
                            // persist html zip to ozone
                            aggregator.htmlOutputStream().finish();
                            aggregator.htmlOutputStream().close();
                            persistFileToOzone(tempHtmlZipFileName);

                            sink.next(aggregator);
                        } catch (IOException e) {
                            log.error("Failed to process file '{}' when uploading to ozone. Reason: '{}'", tempHtmlZipFileName, e.getMessage());
                            sink.error(new UnableToPutObjectToStoreException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage()));
                        }
                    })
                    .<ItemizationResultAggregator>handle((aggregator, sink) -> {
                        log.info("Persisting text zip to Object Store.");
                        try {
                            // persist text zip to ozone
                            aggregator.textOutputStream().finish();
                            aggregator.textOutputStream().close();
                            persistFileToOzone(tempTextZipFileName);

                            sink.next(aggregator);
                        } catch (IOException e) {
                            log.error("Failed to process file '{}' when uploading to ozone. Reason: '{}'", tempTextZipFileName, e.getMessage());
                            sink.error(new UnableToPutObjectToStoreException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage()));
                        }
                    }).block(Duration.ofSeconds(60));

            FileUtils.cleanDirectory(new File(tempFilingDir));
            return Mono.just(
                    new ShortResourceUrlsDto(
                            filingName + typeSuffix.getJson(),
                            filingName + typeSuffix.getHtml(),
                            filingName + typeSuffix.getText()));
        } catch (FileNotFoundException e) {
            log.error("Failed to create zip file, since the zip stream is unable to be opened.");
            throw new FilingProcessException(filingName, HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void persistFileToOzone(String tempZipFileName) throws IOException {

        File file = new File(tempZipFileName);
        log.info("Uploading temp file to S3 - {}", tempZipFileName);
        PutObjectResponse putObjectResult = s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(ozoneConfig.getBucketName())
                        .key(tempZipFileName)
                        .contentLength(file.length())
                        .build(),
                RequestBody.fromFile(file));

        log.info("Successfully upload file '{}' to Ozone. bucketKeyEnabled{}. Status {}. Headers {}",
                tempZipFileName, putObjectResult.bucketKeyEnabled(), putObjectResult.sdkHttpResponse().statusCode(), putObjectResult.sdkHttpResponse().headers());

        // Delete the temporary file
        Files.deleteIfExists(file.toPath());
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
            throw new FilingProcessException(filingName, HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    public Flux<DataBuffer> getItemizedFiling(String filingName) {

        // TODO: download the corresponding file from ozone and stream the result to client.
        // Something to refer: https://www.baeldung.com/java-aws-s3-reactive#1-download-controller.
        // s3Client.getObject(...)

        return null;
    }
}
