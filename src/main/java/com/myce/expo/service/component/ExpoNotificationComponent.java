package com.myce.expo.service.component;

import com.myce.expo.entity.Expo;
import com.myce.expo.entity.type.ExpoStatus;
import com.myce.notification.dto.ExpoStatusChangeCommand;
import com.myce.restclient.service.RestClientService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ExpoNotificationComponent {

    private final RestClientService restClientService;

    public void notifyExpoStatusChange(Expo expo, ExpoStatus oldStatus, ExpoStatus newStatus) {

        ExpoStatusChangeCommand command = commandGenerator(expo, oldStatus, newStatus);

        restClientService.send("notifications/expo-status-changed", command);

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
