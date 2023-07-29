package com.wrdsbackend.tenkbackendservice;

import com.flipkart.hbaseobjectmapper.HBAdmin;
import com.wrdsbackend.tenkbackendservice.config.OzoneConfig;
import com.wrdsbackend.tenkbackendservice.config.TenkAppConfig;
import com.wrdsbackend.tenkbackendservice.dbentity.TenkFilingDbItem;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.hbase.client.Connection;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.util.Optional;

@SpringBootApplication
@OpenAPIDefinition(info = @Info(title = "10-k web service APIs", version = "2.0", description = "Documentation APIs v2.0 for 10-k web service."))
@EnableConfigurationProperties(TenkAppConfig.class)
@RequiredArgsConstructor
@Slf4j
public class TenkBackendServiceApplication {
    private final OzoneConfig ozoneConfig;

    public static void main(String[] args) {
        SpringApplication.run(TenkBackendServiceApplication.class, args);
    }

    @Bean
    // Use this ONLY in dev profile since we have to create table in testing DB and object storage first.
    public CommandLineRunner createHBaseTableIfNotExist(Connection hbaseConnection, S3Client s3Client) {
        return args -> {
            // init hbase
            HBAdmin hbAdmin = HBAdmin.create(hbaseConnection);
            if (!hbAdmin.tableExists(TenkFilingDbItem.class)) {
                hbAdmin.createTable(TenkFilingDbItem.class);
            }
            // init ozone
            String bucketName = ozoneConfig.getBucketName();

            ListBucketsResponse listBucketsResponse = s3Client.listBuckets();
            Optional<Bucket> bucket = listBucketsResponse.buckets().stream()
                    .filter(b -> b.name().equals(bucketName))
                    .findFirst();
            if (bucket.isEmpty()) {
                try {
                    log.info("creating bucket '{}'.", bucketName);
                    s3Client.createBucket(CreateBucketRequest
                            .builder()
                            .bucket(bucketName)
                            .build());

                    log.info("Bucket '{}' created successfully.", bucketName);
                } catch (S3Exception e) {
                    log.error("Unable to create bucket: '{}'.", bucketName);
                    log.error("Error detail: {}.", e.awsErrorDetails().sdkHttpResponse().statusText());
                    System.exit(1);
                }
            }
        };
    }
}
