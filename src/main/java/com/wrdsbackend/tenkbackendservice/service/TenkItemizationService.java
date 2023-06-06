package com.wrdsbackend.tenkbackendservice.service;

import com.wrdsbackend.tenkbackendservice.dto.internal.FilingItemInternalRespDto;
import com.wrdsbackend.tenkbackendservice.dto.internal.FilingItemizationInternalReqDto;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.PostExchange;
import reactor.core.publisher.Flux;

public interface TenkItemizationService {
    @PostExchange (url = "api/internal/tenk/process-html-filing-url")
    Flux<FilingItemInternalRespDto> getFilingItems(@RequestBody FilingItemizationInternalReqDto filingItemizationInternalReqDto);
}
