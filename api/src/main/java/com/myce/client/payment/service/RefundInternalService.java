package com.myce.client.payment.service;

import com.myce.client.payment.PaymentInternalClient;
import com.myce.common.exception.CustomErrorCode;
import com.myce.common.exception.CustomException;
import com.myce.payment.dto.RefundInternalRequest;
import com.myce.payment.dto.RefundInternalResponse;
import com.myce.payment.dto.RefundSumResponse;
import com.myce.payment.entity.type.PaymentTargetType;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RefundInternalService {

    // 같은 PaymentInternalClient를 공유해서 baseUrl 통일
    private final PaymentInternalClient paymentClientService;

    // 즉시 환불 (PortOne 호출 포함)
    public RefundInternalResponse refund(RefundInternalRequest request) {
        ResponseEntity<RefundInternalResponse> response =
                paymentClientService.post("/payment/refund", request, RefundInternalResponse.class);

        if (!response.getStatusCode().equals(HttpStatus.OK)) {
            throw new CustomException(CustomErrorCode.PORTONE_REFUND_FAILED);
        }

        RefundInternalResponse body = response.getBody();
        if (body == null) {
            throw new CustomException(CustomErrorCode.INTERNAL_SERVER_ERROR);
        }

        return body;
    }

    // 환불 신청 (PENDING 생성)
    public RefundInternalResponse requestRefund(RefundInternalRequest request) {
        ResponseEntity<RefundInternalResponse> response =
                paymentClientService.post("/payment/refund-request", request, RefundInternalResponse.class);

        if (!response.getStatusCode().equals(HttpStatus.OK)) {
            throw new CustomException(CustomErrorCode.INTERNAL_SERVER_ERROR);
        }

        RefundInternalResponse body = response.getBody();
        if (body == null) {
            throw new CustomException(CustomErrorCode.INTERNAL_SERVER_ERROR);
        }

        return body;
    }

    // 환불 거절 (PENDING -> REJECTED)
    public RefundInternalResponse rejectRefund(RefundInternalRequest request) {
        ResponseEntity<RefundInternalResponse> response =
                paymentClientService.post("/payment/refund-reject", request, RefundInternalResponse.class);

        if (!response.getStatusCode().equals(HttpStatus.OK)) {
            throw new CustomException(CustomErrorCode.INTERNAL_SERVER_ERROR);
        }

        RefundInternalResponse body = response.getBody();
        if (body == null) {
            throw new CustomException(CustomErrorCode.INTERNAL_SERVER_ERROR);
        }

        return body;
    }

    // targetType/targetId로 impUid 조회
    public String getImpUid(PaymentTargetType targetType, Long targetId) {
        String path = String.format("/payment/imp-uid?targetType=%s&targetId=%d", targetType, targetId);

        ResponseEntity<String> response = paymentClientService.get(path, String.class);
        if (!response.getStatusCode().equals(HttpStatus.OK)) {
            throw new CustomException(CustomErrorCode.PAYMENT_NOT_FOUND);
        }

        String body = response.getBody();
        if (body == null) {
            throw new CustomException(CustomErrorCode.INTERNAL_SERVER_ERROR);
        }

        return body;
    }

    // 환불 상세 조회
    public RefundInternalResponse getRefundByTarget(PaymentTargetType targetType, Long targetId) {
        String path = String.format("/payment/refunds/by-target?targetType=%s&targetId=%d",
                targetType, targetId);

        ResponseEntity<RefundInternalResponse> response =
                paymentClientService.get(path, RefundInternalResponse.class);

        if (!response.getStatusCode().equals(HttpStatus.OK)) {
            throw new CustomException(CustomErrorCode.REFUND_NOT_FOUND);
        }

        RefundInternalResponse body = response.getBody();
        if (body == null) {
            throw new CustomException(CustomErrorCode.INTERNAL_SERVER_ERROR);
        }

        return body;
    }

    // 환불 합계 통계
    public RefundSumResponse sumRefundAmount(PaymentTargetType targetType, LocalDate from, LocalDate to) {
        String path = String.format(
                "/payment/refunds/sum?targetType=%s&from=%s&to=%s",
                targetType, from, to);

        ResponseEntity<RefundSumResponse> response =
                paymentClientService.get(path, RefundSumResponse.class);

        if (!response.getStatusCode().equals(HttpStatus.OK)) {
            throw new CustomException(CustomErrorCode.INTERNAL_SERVER_ERROR);
        }

        RefundSumResponse body = response.getBody();
        if (body == null) {
            throw new CustomException(CustomErrorCode.INTERNAL_SERVER_ERROR);
        }

        return body;
    }
}