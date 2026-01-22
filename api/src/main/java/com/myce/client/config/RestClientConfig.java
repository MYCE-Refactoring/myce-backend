package com.myce.client.config;

import com.myce.auth.security.filter.InternalHeaderKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Value("${external.base-url.notification}")
    private String notificationBaseUrl;

    @Value("${external.base-url.payment}")
    private String paymentBaseUrl;

    @Value("${external.auth.value}")
    private String externalAuthValue;

    @Bean(name = "notificationClient")
    public RestClient notificationClient() {
        return RestClient.builder()
                .baseUrl(notificationBaseUrl)
                .defaultHeader(InternalHeaderKey.INTERNAL_AUTH, externalAuthValue)
                .build();
    }

    @Bean(name = "paymentClient")
    public RestClient paymentClient() {
        return RestClient.builder()
                .baseUrl(paymentBaseUrl)
                .build();
    }
}
