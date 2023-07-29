package com.wrdsbackend.tenkbackendservice.error;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

@Getter
public class FilingProcessException extends ErrorResponseException {
    public FilingProcessException(String filingName) {
        super(HttpStatus.INTERNAL_SERVER_ERROR);
        this.setTitle("Filing processing failed");
        this.setDetail(String.format("Failed to process filing : '%s'.", filingName));
    }
}
