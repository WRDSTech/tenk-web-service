package com.wrdsbackend.tenkbackendservice.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.io.IOException;
import java.net.URI;

@Configuration
@Data
public class OzoneConfig {
    @Value("${ozone.om.host}")
    private String host;
    @Value("${ozone.om.port}")
    private String port;
    @Value("${ozone.om.volume-name}")
    private String volumeName;
    @Value("${ozone.om.bucket-name}")
    private String bucketName;

    @Bean(destroyMethod = "close")
    public S3Client s3Client() {
        return  S3Client.builder()
                // Ozone would ignore this, but this field is needed to set up aws client.
                .region(Region.US_EAST_1)
                // Ozone would ignore this, but this field is needed to set up aws client.
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("dummy-key", "dummy-secret")))
                // Ozone only need this.
                .endpointOverride(URI.create(String.format("http://%s:%s", host, port)))
                // Need to set this to true to do POST operation
                .forcePathStyle(true)
                .build();
    }

    @Bean
    public S3Presigner s3Presigner() {
        return S3Presigner.create();
    }
}
