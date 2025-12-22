package com.myce.qrcode.component;

import com.myce.common.exception.CustomErrorCode;
import com.myce.common.exception.CustomException;
import com.myce.notification.service.NotificationService;
import com.myce.reservation.entity.Reservation;
import com.myce.reservation.entity.code.UserType;
import com.myce.reservation.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class QrIssueComponent {

    private final ReservationRepository reservationRepository;
    private final NotificationService notificationService;

    public void notifyQrIssuedByReservation(Long reservationId, boolean isReissue) {

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new CustomException( CustomErrorCode.RESERVATION_NOT_FOUND));

        if (reservation.getUserType() != UserType.MEMBER) {
            return;
        }

        notificationService.sendQrIssuedNotification(
                reservation.getUserId(),
                reservation.getId(),
                reservation.getExpo().getTitle(),
                isReissue
        );
    }
}
