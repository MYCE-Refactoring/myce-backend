package com.myce.client.payment.service;

import com.myce.client.common.InternalResponseHandler;
import com.myce.client.payment.PaymentInternalClient;
import com.myce.client.payment.dto.PaymentEndPoints;
import com.myce.common.exception.CustomErrorCode;
import com.myce.common.exception.CustomException;
import com.myce.payment.dto.*;
import com.myce.payment.entity.type.PaymentTargetType;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentInternalService {
    private final PaymentInternalClient paymentClientService;

    // 결제 검증/저장 (일반 결제)
    public PaymentInternalResponse verifyAndSave(PaymentInternalRequest request){
        ResponseEntity<PaymentInternalResponse> response =
                paymentClientService.post(PaymentEndPoints.PAYMENT_VERIFY, request, PaymentInternalResponse.class);
        return InternalResponseHandler.requireOk(response, CustomErrorCode.PAYMENT_NOT_PAID);
    }

    // 가상계좌 결제 검증/저장
    public PaymentInternalResponse verifyAndSaveVbank(PaymentInternalRequest request) {
        ResponseEntity<PaymentInternalResponse> response =
                paymentClientService.post(PaymentEndPoints.PAYMENT_VERIFY_VBANK, request, PaymentInternalResponse.class);
        return InternalResponseHandler.requireOk(response, CustomErrorCode.PAYMENT_NOT_READY_OR_PAID);
    }

    /**
     * payment 내부 웹훅 처리 호출
     * - PortOne 재조회는 payment 내부에서 수행
     * - core는 도메인 상태만 갱신
     */
    public PaymentWebhookInternalResponse processWebhook(PaymentWebhookInternalRequest request) {
        // core는 PortOne 호출하지 않고 payment 내부 API만 호출
        ResponseEntity<PaymentWebhookInternalResponse> response =
                paymentClientService.post(PaymentEndPoints.PAYMENT_WEBHOOK, request, PaymentWebhookInternalResponse.class);

        return InternalResponseHandler.requireOk(response, CustomErrorCode.INTERNAL_SERVER_ERROR);
    }

    public PaymentInternalDetailResponse getPaymentByTarget(PaymentTargetType targetType, Long targetId) {
        String path = String.format("%s?targetType=%s&targetId=%d",
                PaymentEndPoints.PAYMENT_BY_TARGET, targetType.name(), targetId);
        try {
            ResponseEntity<PaymentInternalDetailResponse> response =
                    paymentClientService.get(path, PaymentInternalDetailResponse.class);
            return InternalResponseHandler.requireOk(response, CustomErrorCode.INTERNAL_SERVER_ERROR);
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
                paymentClientService.post(PaymentEndPoints.PAYMENT_BY_TARGETS, request, PaymentInternalDetailResponse[].class);
        PaymentInternalDetailResponse[] body =
                InternalResponseHandler.requireOk(response, CustomErrorCode.INTERNAL_SERVER_ERROR);
        return Arrays.asList(body);
    }


}
