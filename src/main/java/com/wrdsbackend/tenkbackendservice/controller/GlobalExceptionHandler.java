package com.wrdsbackend.tenkbackendservice.controller;

import com.wrdsbackend.tenkbackendservice.error.FilingContentFetchException;
import com.wrdsbackend.tenkbackendservice.error.FilingProcessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(FilingContentFetchException.class)
    ProblemDetail handleFilingContentFetchException(FilingContentFetchException e) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
        problemDetail.setTitle("Filing content fetch failed");
        return problemDetail;
    }

    @ExceptionHandler(FilingProcessException.class)
    ProblemDetail handleFilingProcessException(FilingProcessException e) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        problemDetail.setTitle("Filing processing failed");
        return problemDetail;
    }
}
