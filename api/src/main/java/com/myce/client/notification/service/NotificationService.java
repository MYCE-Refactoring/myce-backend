package com.myce.client.notification.service;

import com.myce.client.notification.dto.PaymentCompleteRequest;
import com.myce.client.notification.NotificationClient;
import com.myce.expo.entity.Expo;
import com.myce.expo.entity.type.ExpoStatus;
import com.myce.client.notification.dto.NotificationEndPoints;
import com.myce.client.notification.dto.ExpoStatusChangeCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationClient notificationClient;

    public void sendQrIssuedNotification(Long memberId, Long reservationId, String expoTitle, boolean reissue) {

        Map<String, Object> body = Map.of(
                "memberId", memberId,
                "reservationId", reservationId,
                "expoTitle", expoTitle,
                "reissue", reissue
        );
        notificationClient.send( NotificationEndPoints.QR_ISSUED, body);
    }
    public void sendQrIssuedNotification(Long memberId, Long reservationId, String expoTitle) {
        sendQrIssuedNotification(memberId, reservationId, expoTitle, false);
    }
    public void sendPaymentComplete(PaymentCompleteRequest req) {
        notificationClient.send(NotificationEndPoints.PAYMENT_COMPLETED, req );
    }

    public void notifyExpoStart(List<Long> userIds, Long expoId, String expoTitle) {

        if (userIds.isEmpty()) {
            return;
        }
        Map<String, Object> body = Map.of(
                "memberIds", userIds,
                "expoId", expoId,
                "expoTitle", expoTitle
        );
        notificationClient.send(NotificationEndPoints.EXPO_START, body);

    }

    public void notifyExpoStatusChange(Expo expo, ExpoStatus oldStatus, ExpoStatus newStatus) {
        ExpoStatusChangeCommand command =commandGenerator(expo, oldStatus, newStatus);
        notificationClient.send( NotificationEndPoints.EXPO_STATUS_CHANGED, command);

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

    public void notifyEventHourReminder(List<Long> memberIds, Long expoId, String expoTitle,
                                        String eventName, String startTime
    ) {
        Map<String, Object> body = Map.of(
                "memberIds", memberIds,
                "expoId", expoId,
                "expoTitle", expoTitle,
                "eventName", eventName,
                "startTime", startTime
        );
        notificationClient.send(NotificationEndPoints.EVENT_REMINDER,body);
    }
}
