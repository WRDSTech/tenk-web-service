package com.wrdsbackend.tenkbackendservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ItemizationReqDto {
    @NotNull
    @Pattern(regexp = "^(https?://)?(www\\.)?sec\\.gov.*$",
            message = "The given URL is not valid, it should use https protocol and the domain should be under 'www.sec.gov'")
    @JsonProperty("url")
    private String url;
}
