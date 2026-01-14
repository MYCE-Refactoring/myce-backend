package com.myce.restclient.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class PaymentClientService {

    private final RestClient paymentClient;

    public <T> void send(String path, T body) {
        paymentClient.post()
                .uri(path)
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }
}
