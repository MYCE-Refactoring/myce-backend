package com.myce.client.payment.service;

import com.myce.client.payment.PaymentInternalClient;
import com.myce.common.exception.CustomErrorCode;
import com.myce.common.exception.CustomException;
import com.myce.payment.dto.PaymentInternalRequest;
import com.myce.payment.dto.PaymentInternalResponse;
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
public class PaymentInternalService {
    private final PaymentInternalClient paymentClientService;

    // 결제 검증/저장 (일반 결제)
    public PaymentInternalResponse verifyAndSave(PaymentInternalRequest request){
        ResponseEntity<PaymentInternalResponse> response =
                paymentClientService.post("/payment", request, PaymentInternalResponse.class);
        // 내부 호출도 HTTP 코드 확인
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
}