package com.myce.reservation.entity.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReservationStatus {
    CONFIRMED("예약 확정"),
    CONFIRMED_PENDING("결제 대기"),
    CANCELLED("예약 취소");

    private final String label;

    public static ReservationStatus fromValue(String value) {
        for (ReservationStatus status : ReservationStatus.values()) {
            if (status.name().equalsIgnoreCase(value)) return status;
        }
        throw new IllegalArgumentException("RESERVATION_STATUS_INVALID: " + value);    }

    public static ReservationStatus fromLabel(String label) {
        for (ReservationStatus status : ReservationStatus.values()) {
            if (status.getLabel().equals(label)) return status;
        }
        throw new IllegalArgumentException("RESERVATION_STATUS_INVALID: " + label);
    }
}