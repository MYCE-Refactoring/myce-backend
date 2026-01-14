package com.myce.notification.component;

import com.myce.restclient.service.NotificationClientService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class EventReminderComponent {

    private final NotificationClientService notificationClientService;

    public void notifyEventHourReminder(
            List<Long> memberIds,
            Long expoId,
            String expoTitle,
            String eventName,
            String startTime
    ) {

        Map<String, Object> body = Map.of(
                "memberIds", memberIds,
                "expoId", expoId,
                "expoTitle", expoTitle,
                "eventName", eventName,
                "startTime", startTime
        );

        notificationClientService.send("notifications/event-reminder",body);
    }
}
