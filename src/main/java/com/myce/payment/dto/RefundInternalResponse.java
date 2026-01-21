package com.myce.payment.dto;

import com.myce.payment.entity.type.PaymentTargetType;
import com.myce.payment.entity.type.RefundStatus;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/*
  - 역할: payment internal이 core로 반환하는 환불 결과.
  - 흐름: internal에서 Refund 저장/갱신 후 응답 → core가 받아서 상태 변경/알림에 사용.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundInternalResponse {
    private Long refundId;
    private Long paymentId;

    private PaymentTargetType targetType;
    private Long targetId;

    private Integer refundedAmount;
    private Boolean isPartial;

    private RefundStatus status;
    private LocalDateTime refundedAt;

    // 조회용 추가
    private String reason;             // 환불 사유
    private LocalDateTime requestedAt; // 환불 요청 시각

}
