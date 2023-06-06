package com.wrdsbackend.tenkbackendservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class FilingItem {
    @JsonProperty("item_name")
    private final String itemName;
    @JsonProperty("item_content")
    private final String itemHtmlContent;
}
