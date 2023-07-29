package com.wrdsbackend.tenkbackendservice.controller;

import com.wrdsbackend.tenkbackendservice.error.FilingProcessException;
import com.wrdsbackend.tenkbackendservice.error.UnableToGetFilingResourceException;
import com.wrdsbackend.tenkbackendservice.error.UnexpectedInternalError;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.result.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    @ExceptionHandler(FilingProcessException.class)
    ProblemDetail handleFilingProcessException(FilingProcessException e) {
        return e.getBody();
    }

    @ExceptionHandler(UnableToGetFilingResourceException.class)
    ProblemDetail handleFilingProcessException(UnableToGetFilingResourceException e) {
        return e.getBody();
    }

    @ExceptionHandler(UnexpectedInternalError.class)
    ProblemDetail handleFilingProcessException(UnexpectedInternalError e) {
        return e.getBody();
    }
}
