package com.wrdsbackend.tenkbackendservice.config;

import com.wrdsbackend.tenkbackendservice.dto.internal.FilingItemizationServiceSpecification;
import com.wrdsbackend.tenkbackendservice.dto.internal.StoredItemNameSuffix;
import com.wrdsbackend.tenkbackendservice.service.TenkItemizationService;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.*;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Configuration
@ConfigurationProperties(prefix = "tenk")
@Data
public class TenkAppConfig {
    private Map<String, String> tickerMap;
    @NestedConfigurationProperty
    private FilingItemizationServiceSpecification filingItemizationServiceSpecification;

    private Map<String, String> filingItemToPartNumber;
    @NestedConfigurationProperty
    private StoredItemNameSuffix storedItemNameSuffix;
    private final int ITEMIZATION_SERVICE_TIMEOUT_MILLIS = 15000;
    private final int TEST_URL_VALID_TIMEOUT_MILLIS = 5000;

    public String getfilingProcessingBaseUrl() {
        return String.format("http://%s:%s", filingItemizationServiceSpecification.getHost(), filingItemizationServiceSpecification.getPort());
    }

    @Bean
    public WebClient tenkHttpClient() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, ITEMIZATION_SERVICE_TIMEOUT_MILLIS)
                .responseTimeout(Duration.ofMillis(ITEMIZATION_SERVICE_TIMEOUT_MILLIS))
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(ITEMIZATION_SERVICE_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS))
                                .addHandlerLast(new WriteTimeoutHandler(ITEMIZATION_SERVICE_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)));

        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(1024 * 1024 * 1024))
                .build();

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .exchangeStrategies(strategies)
                .baseUrl(getfilingProcessingBaseUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Bean
    public HttpServiceProxyFactory httpServiceProxyFactory(WebClient tenkHttpClient) {
        return HttpServiceProxyFactory.builder(WebClientAdapter.forClient(tenkHttpClient)).build();
    }

    @Bean
    public TenkItemizationService tenkItemizationService(HttpServiceProxyFactory factory) {
        return factory.createClient(TenkItemizationService.class);
    }

    @Bean
    public RestTemplate tenkRestTemplate() {
        return new RestTemplate();
    }
    
    @Bean
    public HttpEntity<String> tenkHttpEntity() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9");
        headers.set("Accept-Encoding", "gzip, deflate, br");
        headers.set("Accept-Language", "zh-CN,zh;q=0.9,en-US;q=0.8,en;q=0.7,zh-TW;q=0.6");
        headers.set("Dnt", "1");
        headers.set("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/109.0.0.0 Safari/537.36");
        return new HttpEntity<>(null, headers);
    }
}
