package com.myce.client.config;

import com.myce.auth.security.filter.InternalHeaderKey;
import com.myce.common.exception.CustomErrorCode;
import com.myce.common.exception.CustomException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Value("${external.base-url.notification}")
    private String notificationBaseUrl;

    @Value("${external.base-url.payment}")
    private String paymentBaseUrl;

    @Value("${external.auth.value}")
    private String externalAuthValue;

    @Bean(name = "notificationBuilder")
    @LoadBalanced
    public RestClient.Builder notificationBuilder() {
        return RestClient.builder();
    }

    @Bean(name = "paymentBuilder")
    @LoadBalanced
    public RestClient.Builder paymentBuilder() {
        return RestClient.builder();
    }

    @Bean(name = "notificationClient")
    public RestClient notificationClient(@Qualifier("notificationBuilder") RestClient.Builder builder) {
        return builder
                .baseUrl(notificationBaseUrl)
                .defaultHeader(InternalHeaderKey.INTERNAL_AUTH, externalAuthValue)
                .build();
    }

    @Bean(name = "paymentClient")
    public RestClient paymentClient(@Qualifier("paymentBuilder") RestClient.Builder builder) {
        return builder
                .baseUrl(paymentBaseUrl)
                .defaultHeader(InternalHeaderKey.INTERNAL_AUTH, externalAuthValue)
                .build();
    }

}
