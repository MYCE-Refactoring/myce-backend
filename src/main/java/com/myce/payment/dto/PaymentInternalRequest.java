package com.myce.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Payment Internal API 요청 DTO
 * - 포트원 검증에 필요한 최소 정보만 포함
 * - Payment 저장에 필요한 정보만 포함
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentInternalRequest{
    // 포트원 결제 정보
    private String impUid;        // 포트원 결제 고유번호
    private String merchantUid;   // 가맹점 주문번호
    private Integer amount;       // 결제 금액

    // 예약 정보
    private Long reservationId;   // 이미 생성된 Reservation의 ID
}