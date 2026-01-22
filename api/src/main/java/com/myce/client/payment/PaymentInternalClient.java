package com.myce.client.payment;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class PaymentInternalClient {
    // RestClientConfig에서 만든 paymentClient Bean 주입
    private final RestClient paymentClient;

    // 응답을 받을 필요 없는 내부 호출용 (fire-and-forget)
    public <T> void send(String path, T body) {
        paymentClient.post()
                .uri(path)
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }

    // TODO status exception 처리 해야한다 -> 상태 처리 에러 헨들링
    // 응답 DTO가 필요한 내부 호출용 -> POST
    public <T, R> ResponseEntity<R> post(String path, T body, Class<R> responseType) {
        return paymentClient.post()
                .uri(path)
                .body(body)
                .retrieve()
                .toEntity(responseType);
    }

    // 조회용 내부 호출 (GET)
    //  refund 조회/통계는 GET이 필요하므로 추가
    public <R> ResponseEntity<R> get(String path, Class<R> responseType) {
        return paymentClient.get()
                .uri(path)
                .retrieve()
                .toEntity(responseType);
    }
}