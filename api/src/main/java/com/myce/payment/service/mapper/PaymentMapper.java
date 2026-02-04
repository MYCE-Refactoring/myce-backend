package com.myce.payment.service.mapper;

import com.myce.payment.entity.ReservationPaymentInfo;
import com.myce.payment.entity.type.PaymentStatus;
import com.myce.reservation.entity.Reservation;
import org.springframework.stereotype.Component;

@Component
public class PaymentMapper {

    public ReservationPaymentInfo toReservationPaymentInfo(Reservation reservation, Integer paidAmount,
                                    PaymentStatus paymentStatus, int usedMileage, int savedMileage) {
        return ReservationPaymentInfo.builder()
            .reservation(reservation)
            .totalAmount(paidAmount)
            .status(paymentStatus)
            .usedMileage(usedMileage)
            .savedMileage(savedMileage)
            .build();
    }
}
