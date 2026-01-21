package com.myce.client.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

//  - yml에서 주소 읽어옴 → 환경 바뀌어도 코드 수정 없음
//  - paymentClient가 내부 API 호출의 핵심
@Configuration
public class RestClientConfig {


    @Value("${external.base-url.notification}")
    private String notificationBaseUrl;
    // application.yml의 external.base-url.payment 값을 읽어
    @Value("${external.base-url.payment}")
    private String paymentBaseUrl;

    @Bean(name = "notificationClient")
    public RestClient notificationClient() {
        return RestClient.builder()
                .baseUrl(notificationBaseUrl)
                .build();
    }

    @Bean(name = "paymentClient")  // payment 전용 RestClient Bean
    public RestClient paymentClient() {
        return RestClient.builder()
                .baseUrl(paymentBaseUrl)
                .build();
    }


}
