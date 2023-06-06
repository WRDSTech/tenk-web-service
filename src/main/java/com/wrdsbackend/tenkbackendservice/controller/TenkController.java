package com.wrdsbackend.tenkbackendservice.controller;

import com.wrdsbackend.tenkbackendservice.config.TenkAppConfig;
import com.wrdsbackend.tenkbackendservice.dto.ItemizationReqDto;
import com.wrdsbackend.tenkbackendservice.dto.ItemizedFormReqDto;
import com.wrdsbackend.tenkbackendservice.dto.ShortResourceUrlsDto;
import com.wrdsbackend.tenkbackendservice.service.TenkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api/tenk")
@RequiredArgsConstructor
@Slf4j
public class TenkController {

    private final TenkAppConfig tenkAppConfig;
    private final TenkService tenkService;

    @GetMapping("/samples")
    @Operation(description = "Get an sample of company ticker name to filing URL mapping.")
    public Map<String, String> getSampleList() {
        return tenkAppConfig.getTickerMap();
    }

    @GetMapping("/sample-form")
    @Operation(description = "Get an itemized filing resource with filing name.", parameters = {
            @Parameter(name = "filing_name", in = ParameterIn.QUERY, required = true, description = "filing name with content type suffix.")
    })
    public Flux<DataBuffer> getItemizedFiling(@Valid @RequestParam("filing_name") ItemizedFormReqDto req) {
        return tenkService.getItemizedFiling(req.getFilingName());
    }

    @PostMapping("/itemization")
    @Operation(
            description = "Itemize a filing from the given filing URL.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "link to 10-k text filing.",
                    content = @Content(
                            schema = @Schema(
                                    implementation = ItemizationReqDto.class),
                            examples = @ExampleObject(
                                    name = "sample 10-k filing url.",
                                    value = "{\"url\": \"https://www.sec.gov/Archives/edgar/data/320193/000032019322000108/0000320193-22-000108.txt\"}"
                            ),
                    mediaType = "application/json")
            ))
    @ApiResponses({
            @ApiResponse(responseCode = "200", content = {@Content(
                    schema = @Schema(implementation = ShortResourceUrlsDto.class),
                    mediaType = "application/json",
                    examples =@ExampleObject(
                            name = "sample response.",
                            value = "{\n" +
                                    "    \"json_link\": \"0000320193-22-000108_json\",\n" +
                                    "    \"html_link\": \"0000320193-22-000108_html\",\n" +
                                    "    \"text_link\": \"0000320193-22-000108_text\"\n" +
                                    "}"
                    )
            )}),
            @ApiResponse(responseCode = "404", content = {@Content(schema = @Schema())},
                    description = "Given URL does not map to any existing filing resources."),
            @ApiResponse(responseCode = "400", content = {@Content(schema = @Schema())},
                    description = "Bad request. This may suggest the URL format is not valid."),
            @ApiResponse(responseCode = "500", content = {@Content(schema = @Schema())},
                    description = "Internal Server Error.")
    })
    public Mono<ShortResourceUrlsDto> itemizeFilingFromUrl(@Valid @RequestBody ItemizationReqDto req) {
        return tenkService.processFilingURL(req.getUrl());
    }
}
