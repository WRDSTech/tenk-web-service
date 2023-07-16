package com.wrdsbackend.tenkbackendservice.error;

import org.springframework.http.HttpStatus;

public class UnableToFetchItemsFromDbException extends RuntimeException {
    public UnableToFetchItemsFromDbException(HttpStatus status, String reason) {
        super(String.format(
                "An error has occurred. Got: '%s'. Reason: '%s'", status, reason));
    }
}
