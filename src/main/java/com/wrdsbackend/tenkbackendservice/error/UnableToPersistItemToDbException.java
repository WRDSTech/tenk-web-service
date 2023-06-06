package com.wrdsbackend.tenkbackendservice.error;

import org.springframework.http.HttpStatus;

public class UnableToPersistItemToDbException extends RuntimeException {
    public UnableToPersistItemToDbException(HttpStatus status, String reason) {
        super(String.format(
                "Unable to persist to DB. Got: '%s'. Reason: '%s'", status, reason));
    }
}
