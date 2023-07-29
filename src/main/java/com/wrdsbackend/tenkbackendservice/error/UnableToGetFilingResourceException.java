package com.wrdsbackend.tenkbackendservice.error;

import lombok.Getter;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.ErrorResponseException;

@Getter
public class UnableToGetFilingResourceException extends ErrorResponseException {
    public UnableToGetFilingResourceException(String url, HttpStatusCode statusCode) {
        super(statusCode);
        this.setTitle("Unable to get filing resource");
        this.setDetail(String.format("Unable to get Filing Resource from the given URL '%s'", url));
    }

}
