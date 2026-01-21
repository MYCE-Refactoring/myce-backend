package com.myce.notification.component;

import com.myce.expo.entity.Expo;
import com.myce.expo.entity.type.ExpoStatus;
import com.myce.notification.component.endpoints.NotificationEndPoints;
import com.myce.notification.dto.ExpoStatusChangeCommand;
import com.myce.restclient.service.NotificationClientService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ExpoNotificationComponent {

    private final NotificationClientService notificationClientService;

    public void notifyExpoStatusChange(Expo expo, ExpoStatus oldStatus, ExpoStatus newStatus) {

        ExpoStatusChangeCommand command = commandGenerator(expo, oldStatus, newStatus);

        notificationClientService.send( NotificationEndPoints.EXPO_STATUS_CHANGED, command);

    }

    private ExpoStatusChangeCommand commandGenerator(Expo expo, ExpoStatus oldStatus, ExpoStatus newStatus){

        return ExpoStatusChangeCommand.builder()
                .memberId(expo.getMember().getId())
                .expoId(expo.getId())
                .expoTitle(expo.getTitle())
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .build();
    }
}
