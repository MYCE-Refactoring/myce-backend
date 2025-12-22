package com.myce.expo.service.component;

import com.myce.expo.entity.Expo;
import com.myce.notification.service.NotificationService;
import com.myce.reservation.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ExpoReminderComponent {

    private final ReservationRepository reservationRepository;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public void notifyExpoStart(Expo expo) {

        List<Long> userIds =
                reservationRepository.findDistinctUserIdsByExpoId(expo.getId());

        if (userIds.isEmpty()) {
            return;
        }

        notificationService.sendExpoStartNotification(
                userIds,
                expo.getTitle(),
                expo.getId()
        );
    }
}
