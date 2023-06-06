package com.wrdsbackend.tenkbackendservice.dto.internal;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class FilingItemInternalRespDto {
    @JsonProperty("item_name")
    private String itemName;
    @JsonProperty("item_html")
    private String itemHtmlContent;
    @JsonProperty("item_text")
    private String itemTextContent;
}
