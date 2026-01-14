package com.myce.notification.component;

import com.myce.common.exception.CustomErrorCode;
import com.myce.common.exception.CustomException;
import com.myce.reservation.entity.Reservation;
import com.myce.reservation.entity.code.UserType;
import com.myce.reservation.repository.ReservationRepository;
import com.myce.restclient.service.NotificationClientService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class QrIssueComponent {

    private final ReservationRepository reservationRepository;
    private final NotificationClientService notificationClientService;

    public void notifyQrIssuedByReservation(Long reservationId, boolean isReissue) {

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new CustomException( CustomErrorCode.RESERVATION_NOT_FOUND));

        if (reservation.getUserType() != UserType.MEMBER) {
            return;
        }

        Map<String, Object> body = Map.of(
                "memberId", reservation.getUserId(),
                "reservationId", reservation.getId(),
                "expoTitle", reservation.getExpo().getTitle(),
                "reissue", isReissue
        );

        notificationClientService.send("notifications/qr-issued",body);
    }
}
