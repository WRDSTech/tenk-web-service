package com.wrdsbackend.tenkbackendservice.error;

import org.springframework.http.HttpStatusCode;

public class FilingContentFetchException extends RuntimeException{
    public FilingContentFetchException(String url, HttpStatusCode code) {
        super(String.format("Failed to fetch filing content from given url: %s, status code: %d", url, code.value()));
    }
}
