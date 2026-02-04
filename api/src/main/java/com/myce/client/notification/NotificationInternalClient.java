package com.myce.client.notification;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class NotificationInternalClient {

    private final RestClient notificationClient;

    public <T> ResponseEntity<Void> send(String path, T body) {
        return notificationClient.post()
                .uri(path)
                .body(body)
                .exchange((req, res) -> ResponseEntity
                        .status(res.getStatusCode())
                        .build() );
    }

    public <T> T receive(String path, Class<T> responseType) {
        return notificationClient.get()
                .uri(path)
                .retrieve()
                .body(responseType);
    }

}
