package com.wrdsbackend.tenkbackendservice.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ItemizedFormReqDto {
    @NotNull
    @Pattern(regexp = "^[0-9-]+_(json|html|text)$",
            message = "must be a filing name followed by a type suffix. The supported types are json, html, txt.")
    private String filingName;
}
