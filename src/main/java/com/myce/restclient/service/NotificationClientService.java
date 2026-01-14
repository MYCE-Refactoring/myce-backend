package com.myce.restclient.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class NotificationClientService {

    private final RestClient notificationClient;

    public <T> void send(String path, T body) {
        notificationClient.post()
                .uri(path)
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }

    public <T> T receive(String path, Class<T> responseType) {
        return notificationClient.get()
                .uri(path)
                .retrieve()
                .body(responseType);
    }

}
