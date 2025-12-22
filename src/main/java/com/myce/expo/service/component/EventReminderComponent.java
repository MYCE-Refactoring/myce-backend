package com.myce.expo.service.component;

import com.myce.expo.entity.Expo;
import com.myce.expo.entity.type.ExpoStatus;
import com.myce.notification.dto.ExpoStatusChangeCommand;
import com.myce.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class EventReminderComponent {

    private final NotificationService notificationService;

    public void notifyEventHourReminder(
            List<Long> memberIds,
            Long expoId,
            String expoTitle,
            String eventName,
            String startTime
    ) {
        notificationService.sendEventHourReminderNotification(
                memberIds,
                expoId,
                expoTitle,
                eventName,
                startTime
        );
    }
}
