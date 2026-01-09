package com.myce.expo.service.component;

import com.myce.expo.entity.Expo;
import com.myce.expo.entity.type.ExpoStatus;
import com.myce.notification.dto.ExpoStatusChangeCommand;
import com.myce.notification.service.NotificationService;
import com.myce.restclient.service.RestClientService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class EventReminderComponent {

    private final RestClientService restclientService;

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

        restclientService.send("notifications/event-reminder",body);
    }
}
