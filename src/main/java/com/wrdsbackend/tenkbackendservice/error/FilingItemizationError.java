package com.wrdsbackend.tenkbackendservice.error;


import org.springframework.http.HttpStatusCode;

public class FilingItemizationError extends RuntimeException {
    public FilingItemizationError(String url, HttpStatusCode statusCode, String reason) {
        super(String.format(
                "Failed to process filing content from given url: '%s'. Got: '%s'. Reason: '%s'",
                url, statusCode, reason));
    }
}
