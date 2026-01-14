package com.myce.notification.component;

import com.myce.expo.entity.Expo;
import com.myce.reservation.repository.ReservationRepository;
import com.myce.restclient.service.NotificationClientService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ExpoReminderComponent {

    private final ReservationRepository reservationRepository;
    private final NotificationClientService restclient;

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

        restclient.send("notifications/event-reminder", body);

    }
}
