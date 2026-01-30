package com.myce.client.payment.service;

import com.myce.client.common.InternalResponseHandler;
import com.myce.client.payment.PaymentInternalClient;
import com.myce.client.payment.dto.PaymentEndPoints;
import com.myce.common.exception.CustomErrorCode;
import com.myce.common.exception.CustomException;
import com.myce.payment.dto.RefundInternalRequest;
import com.myce.payment.dto.RefundInternalResponse;
import com.myce.payment.dto.RefundSumResponse;
import com.myce.payment.entity.type.PaymentTargetType;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RefundInternalService {

    // 같은 PaymentInternalClient를 공유해서 baseUrl 통일
    private final PaymentInternalClient paymentClientService;

    // 즉시 환불 (payment internal에서 PortOne 호출 포함)
    public RefundInternalResponse refund(RefundInternalRequest request) {
        ResponseEntity<RefundInternalResponse> response =
                paymentClientService.post(PaymentEndPoints.REFUND, request, RefundInternalResponse.class);

        return InternalResponseHandler.requireOk(response, CustomErrorCode.PORTONE_REFUND_FAILED);
    }

    // 환불 신청 (PENDING 생성)
    public RefundInternalResponse requestRefund(RefundInternalRequest request) {
        ResponseEntity<RefundInternalResponse> response =
                paymentClientService.post(PaymentEndPoints.REFUND_REQUEST, request, RefundInternalResponse.class);

        return InternalResponseHandler.requireOk(response, CustomErrorCode.INTERNAL_SERVER_ERROR);
    }

    // 환불 거절 (PENDING -> REJECTED)
    public RefundInternalResponse rejectRefund(RefundInternalRequest request) {
        ResponseEntity<RefundInternalResponse> response =
                paymentClientService.post(PaymentEndPoints.REFUND_REJECT, request, RefundInternalResponse.class);

        return InternalResponseHandler.requireOk(response, CustomErrorCode.INTERNAL_SERVER_ERROR);
    }

    // targetType/targetId로 impUid 조회
    public String getImpUid(PaymentTargetType targetType, Long targetId) {
        String path = String.format("%s?targetType=%s&targetId=%d", PaymentEndPoints.IMP_UID, targetType, targetId);

        ResponseEntity<String> response = paymentClientService.get(path, String.class);
        return InternalResponseHandler.requireOk(response, CustomErrorCode.PAYMENT_NOT_FOUND);
    }

    // 환불 상세 조회
    public RefundInternalResponse getRefundByTarget(PaymentTargetType targetType, Long targetId) {
        String path = String.format("%s?targetType=%s&targetId=%d",
                PaymentEndPoints.REFUND_BY_TARGET, targetType, targetId);

        ResponseEntity<RefundInternalResponse> response =
                paymentClientService.get(path, RefundInternalResponse.class);

        return InternalResponseHandler.requireOk(response, CustomErrorCode.REFUND_NOT_FOUND);
    }

    // 환불 합계 통계
    public RefundSumResponse sumRefundAmount(PaymentTargetType targetType, LocalDate from, LocalDate to) {
        String path = String.format(
                "%s?targetType=%s&from=%s&to=%s",
                PaymentEndPoints.REFUND_SUM, targetType, from, to);

        ResponseEntity<RefundSumResponse> response =
                paymentClientService.get(path, RefundSumResponse.class);

        return InternalResponseHandler.requireOk(response, CustomErrorCode.INTERNAL_SERVER_ERROR);
    }
}
