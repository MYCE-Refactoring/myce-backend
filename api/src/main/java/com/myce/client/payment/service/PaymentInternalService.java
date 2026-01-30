package com.myce.client.payment.service;

import com.myce.client.payment.PaymentInternalClient;
import com.myce.common.exception.CustomErrorCode;
import com.myce.common.exception.CustomException;
import com.myce.payment.dto.PaymentInternalDetailResponse;
import com.myce.payment.dto.PaymentInternalRequest;
import com.myce.payment.dto.PaymentInternalResponse;
import com.myce.payment.dto.PaymentInternalTargetRequest;
import com.myce.payment.dto.PaymentInternalTargetsRequest;
import com.myce.payment.dto.PaymentWebhookInternalRequest;
import com.myce.payment.dto.PaymentWebhookInternalResponse;
import com.myce.payment.entity.type.PaymentTargetType;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentInternalService {
    private final PaymentInternalClient paymentClientService;

    // 결제 검증/저장 (일반 결제)
    public PaymentInternalResponse verifyAndSave(PaymentInternalRequest request){
        ResponseEntity<PaymentInternalResponse> response =
                paymentClientService.post("/payment", request, PaymentInternalResponse.class);
        // 내부 호출도 HTTP 코드 확인
        // TODO: http status 에러 헨들링 config에서 한번에 처리
        if (!response.getStatusCode().equals(HttpStatus.OK)) {
            throw new CustomException(CustomErrorCode.PAYMENT_NOT_PAID);
        }
        // body가 없으면 서버 내부 오류로 처리
        PaymentInternalResponse body = response.getBody();
        if (body == null) {
            throw new CustomException(CustomErrorCode.INTERNAL_SERVER_ERROR);
        }
        return body;
    }

    // 가상계좌 결제 검증/저장
    public PaymentInternalResponse verifyAndSaveVbank(PaymentInternalRequest request) {
        ResponseEntity<PaymentInternalResponse> response =
                paymentClientService.post("/payment/vbank", request, PaymentInternalResponse.class);
        // vbank는 ready/paid 상태만 허용
        if (!response.getStatusCode().equals(HttpStatus.OK)) {
            throw new CustomException(CustomErrorCode.PAYMENT_NOT_READY_OR_PAID);
        }

        PaymentInternalResponse body = response.getBody();
        if (body == null) {
            throw new CustomException(CustomErrorCode.INTERNAL_SERVER_ERROR);
        }
        return body;
    }

    /**
     * payment 내부 웹훅 처리 호출
     * - PortOne 재조회는 payment 내부에서 수행
     * - core는 도메인 상태만 갱신
     */
    public PaymentWebhookInternalResponse processWebhook(PaymentWebhookInternalRequest request) {
        // core는 PortOne 호출하지 않고 payment 내부 API만 호출
        ResponseEntity<PaymentWebhookInternalResponse> response =
                paymentClientService.post("/payment/webhook", request, PaymentWebhookInternalResponse.class);

        if (!response.getStatusCode().equals(HttpStatus.OK)) {
            throw new CustomException(CustomErrorCode.INTERNAL_SERVER_ERROR);
        }
        if (response.getBody() == null) {
            throw new CustomException(CustomErrorCode.INTERNAL_SERVER_ERROR);
        }
        return response.getBody();
    }

    public PaymentInternalDetailResponse getPaymentByTarget(PaymentTargetType targetType, Long targetId) {
        String path = String.format("/payment/by-target?targetType=%s&targetId=%d",
                targetType.name(), targetId);
        try {
            ResponseEntity<PaymentInternalDetailResponse> response =
                    paymentClientService.get(path, PaymentInternalDetailResponse.class);
            if (!response.getStatusCode().equals(HttpStatus.OK)) {
                throw new CustomException(CustomErrorCode.INTERNAL_SERVER_ERROR);
            }
            return response.getBody();
        } catch (RestClientResponseException e) {
            if (e.getRawStatusCode() == HttpStatus.NOT_FOUND.value()) {
                return null;
            }
            throw new CustomException(CustomErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    public List<PaymentInternalDetailResponse> getPaymentsByTargets(List<PaymentInternalTargetRequest> targets) {
        if (targets == null || targets.isEmpty()) {
            return Collections.emptyList();
        }
        PaymentInternalTargetsRequest request = PaymentInternalTargetsRequest.builder()
                .targets(targets)
                .build();
        ResponseEntity<PaymentInternalDetailResponse[]> response =
                paymentClientService.post("/payment/by-targets", request, PaymentInternalDetailResponse[].class);
        if (!response.getStatusCode().equals(HttpStatus.OK)) {
            throw new CustomException(CustomErrorCode.INTERNAL_SERVER_ERROR);
        }
        PaymentInternalDetailResponse[] body = response.getBody();
        if (body == null) {
            return Collections.emptyList();
        }
        return Arrays.asList(body);
    }


}
