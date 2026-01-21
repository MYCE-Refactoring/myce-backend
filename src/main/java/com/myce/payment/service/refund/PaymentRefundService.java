package com.myce.payment.service.refund;

import com.myce.payment.dto.AdRefundRequest;
import com.myce.payment.dto.PaymentImpUidForRefundRequest;
import com.myce.payment.dto.PaymentRefundRequest;
import com.myce.payment.dto.RefundInternalResponse;

public interface PaymentRefundService {

    // 환불 실행 결과를 internal 응답 DTO로 통일
    RefundInternalResponse refundPayment(PaymentRefundRequest request);

    // targetType/targetId → impUid 조회 (internal 조회로 이동)
    String getImpUidForRefund(PaymentImpUidForRefundRequest request);

    // 광고 환불 결과도 internal 응답 DTO로 통일
    RefundInternalResponse processAdRefund(AdRefundRequest request);
}