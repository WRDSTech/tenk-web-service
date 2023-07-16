package com.wrdsbackend.tenkbackendservice.dto.internal;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@ConfigurationProperties("tenk.filing-process-service")
@Component
public class FilingItemizationServiceSpecification {
    private String host;
    private String port;
    private String path;
}
