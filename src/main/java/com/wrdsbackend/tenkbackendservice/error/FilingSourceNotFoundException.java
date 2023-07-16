package com.wrdsbackend.tenkbackendservice.error;

import org.springframework.http.HttpStatus;

public class FilingSourceNotFoundException extends RuntimeException {
    public FilingSourceNotFoundException(String url, HttpStatus status) {
        super(String.format("Failed to fetch filing resource : '%s'. Got: '%s'. Check if the filing resource exist.",
                url, status));
    }
}
