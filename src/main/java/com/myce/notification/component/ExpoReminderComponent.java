package com.myce.notification.component;

import com.myce.restclient.service.NotificationClientService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ExpoReminderComponent {

    private final NotificationClientService notificationClientService;

    public void notifyExpoStart(List<Long> userIds, Long expoId, String expoTitle) {

        if (userIds.isEmpty()) {
            return;
        }

        Map<String, Object> body = Map.of(
                "memberIds", userIds,
                "expoId", expoId,
                "expoTitle", expoTitle
        );

        notificationClientService.send("notifications/event-reminder", body);

    }
}
