package com.myce.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/*
  - 역할: core → payment internal로 보내는 환불 요청 바디.
  - 흐름: core 서비스에서 DTO 생성 → /internal/payment/refund 또는 /refund-request 호출.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundInternalRequest {
    // 식별자: paymentId > impUid > merchantUid 순으로 사용
    private Long paymentId;
    private String impUid;
    private String merchantUid;

    // null이면 전액 환불
    private Integer cancelAmount;

    // 환불 사유
    private String reason;

    // 가상계좌 환불 정보 (필요 시)
    private String refundHolder;
    private String refundBank;
    private String refundAccount;
    private String refundTel;
}

