package com.myce.expo.service.component;

import com.myce.expo.entity.Expo;
import com.myce.notification.service.NotificationService;
import com.myce.reservation.repository.ReservationRepository;
import com.myce.restclient.service.RestClientService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ExpoReminderComponent {

    private final ReservationRepository reservationRepository;
    private final NotificationService notificationService;
    private final RestClientService restclientService;

    @Transactional(readOnly = true)
    public void notifyExpoStart(Expo expo) {

        List<Long> userIds =
                reservationRepository.findDistinctUserIdsByExpoId(expo.getId());

        if (userIds.isEmpty()) {
            return;
        }

        Map<String, Object> body = Map.of(
                "memberIds", userIds,
                "expoId", expo.getId(),
                "expoTitle", expo.getTitle()
        );

        restclientService.send("notifications/event-reminder", body);

    }
}
