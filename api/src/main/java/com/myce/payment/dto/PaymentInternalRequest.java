package com.myce.payment.dto;

import com.myce.payment.entity.type.PaymentTargetType;
import jakarta.validation.constraints.NotNull;
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
    private Long reservationId;   // 이미 생성된 Reservation의 ID (레거시/조건부)

    /**
     * 결제 라우팅에 필요한 필수 값.
     * - targetType/targetId는 항상 함께 전달되어야 함.
     * - reservationId는 RESERVATION 케이스에서만 보조적으로 사용.
     */
    @NotNull
    private PaymentTargetType targetType;

    @NotNull
    private Long targetId;

}
