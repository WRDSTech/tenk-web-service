package com.wrdsbackend.tenkbackendservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ShortResourceUrlsDto {
    @JsonProperty("json_link")
    private final String jsonShortUrl;
    @JsonProperty("html_link")
    private final String htmlShortUrl;
    @JsonProperty("text_link")
    private final String textShortUrl;
}
