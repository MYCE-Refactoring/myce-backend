package com.myce.member.mapper;

import com.myce.member.dto.PaymentHistoryResponse;
import com.myce.payment.dto.PaymentInternalDetailResponse;
import com.myce.payment.entity.ReservationPaymentInfo;
import com.myce.reservation.entity.Reservation;
import org.springframework.stereotype.Component;

@Component
public class PaymentHistoryMapper {
    
    public PaymentHistoryResponse toResponseDto(Reservation reservation,
                                                ReservationPaymentInfo paymentInfo,
                                                PaymentInternalDetailResponse payment) {
        if (reservation == null || paymentInfo == null || payment == null) {
            return null;
        }
        return PaymentHistoryResponse.builder()
                .paymentNumber(payment.getImpUid())
                .paymentDate(payment.getCreatedAt())
                .expoTitle(reservation.getExpo().getTitle())
                .totalAmount(paymentInfo.getTotalAmount())
                .status(paymentInfo.getStatus())
                .reservationCode(reservation.getReservationCode())
                .build();
    }
}
