package com.myce.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/*
  - 역할: 환불 합계 통계 응답.
  - 흐름: /internal/payment/refunds/sum 호출 → totalAmount 반환.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundSumResponse {
    private Long totalAmount;
}