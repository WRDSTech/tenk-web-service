package com.wrdsbackend.tenkbackendservice.error;

import org.springframework.http.HttpStatus;

public class UnableToPutObjectToStoreException extends RuntimeException {
    public UnableToPutObjectToStoreException(HttpStatus status, String reason) {
        super(String.format(
                "Unable to put object to the store. Got: '%s'. Reason: '%s'", status, reason));
    }
}
