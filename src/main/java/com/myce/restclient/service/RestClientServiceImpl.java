package com.myce.restclient.service;

import com.myce.restclient.config.RestClientConfig;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
@Transactional
public class RestClientServiceImpl implements RestClientService{

    private final RestClient restClient;

    @Override
    public <T> void send(String path, T body) {
        restClient.post()
                .uri(path)
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }
}
