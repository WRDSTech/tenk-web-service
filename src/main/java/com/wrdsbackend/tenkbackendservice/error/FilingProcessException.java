package com.wrdsbackend.tenkbackendservice.error;


import io.netty.handler.codec.http.HttpResponseStatus;
import org.springframework.http.HttpStatus;

public class FilingProcessException extends RuntimeException {
    public FilingProcessException(String url, HttpStatus status, String reason) {
        super(String.format("Failed to process filing : '%s'. Got: '%s'. Reason: '%s'",
                url, status, reason));
    }
}
