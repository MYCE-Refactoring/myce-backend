package com.myce.payment.dto;

import com.myce.payment.entity.type.PaymentTargetType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentWebhookInternalResponse {
    // 포트원 재조회 결과를 코어가 판단할 수 있게 전달
    private String impUid;
    private String merchantUid;
    private String status;        // paid 여부 판단용
    private Integer paidAmount;   // 금액 검증용
    private Long paidAt;          // 로그/추적용 (unix timestamp)
    private PaymentTargetType targetType; // 도메인 분기용
    private Long targetId;               // 도메인 분기용
}
