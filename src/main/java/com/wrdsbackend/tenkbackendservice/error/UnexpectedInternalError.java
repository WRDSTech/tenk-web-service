package com.wrdsbackend.tenkbackendservice.error;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.ErrorResponseException;

public class UnexpectedInternalError extends ErrorResponseException {
    public UnexpectedInternalError() {
        super(HttpStatus.INTERNAL_SERVER_ERROR);
        this.setTitle("Unexpected Internal Error");
        this.setDetail("An unknown internal error occurred, please try again later.");
    }
}
