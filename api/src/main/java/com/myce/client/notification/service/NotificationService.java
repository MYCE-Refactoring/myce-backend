package com.myce.client.notification.service;

import com.myce.advertisement.entity.Advertisement;
import com.myce.advertisement.entity.type.AdvertisementStatus;
import com.myce.client.notification.dto.AdStatusChangeCommand;
import com.myce.client.notification.dto.PaymentCompleteRequest;
import com.myce.client.notification.NotificationInternalClient;
import com.myce.common.exception.CustomErrorCode;
import com.myce.common.exception.CustomException;
import com.myce.expo.entity.Expo;
import com.myce.expo.entity.type.ExpoStatus;
import com.myce.client.notification.dto.NotificationEndPoints;
import com.myce.client.notification.dto.ExpoStatusChangeCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationInternalClient notificationInternalClient;

    public void sendQrIssuedNotification(Long memberId, Long reservationId, String expoTitle, boolean reissue) {

        Map<String, Object> body = Map.of(
                "memberId", memberId,
                "reservationId", reservationId,
                "expoTitle", expoTitle,
                "reissue", reissue
        );
        ResponseEntity<Void> res = notificationInternalClient.send(NotificationEndPoints.QR_ISSUED, body);

        if (!res.getStatusCode().is2xxSuccessful()) {
            throw new CustomException(CustomErrorCode.NOTIFICATION_SAVE_FAILED);
        }
    }

    public void sendQrIssuedNotification(Long memberId, Long reservationId, String expoTitle) {
        sendQrIssuedNotification(memberId, reservationId, expoTitle, false);
    }
    public void sendPaymentComplete(PaymentCompleteRequest req) {
        ResponseEntity<Void> res = notificationInternalClient.send(NotificationEndPoints.PAYMENT_COMPLETED, req);

        if (!res.getStatusCode().is2xxSuccessful()) {
            throw new CustomException( CustomErrorCode.NOTIFICATION_SAVE_FAILED);
        }
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
        ResponseEntity<Void> res = notificationInternalClient.send(NotificationEndPoints.EXPO_START, body);

        if (!res.getStatusCode().is2xxSuccessful()) {
            throw new CustomException( CustomErrorCode.NOTIFICATION_SAVE_FAILED);
        }

    }

    public void notifyExpoStatusChange(Expo expo, ExpoStatus oldStatus, ExpoStatus newStatus) {
        ExpoStatusChangeCommand command= commandGenerator(expo, oldStatus, newStatus);
        ResponseEntity<Void> res = notificationInternalClient.send(NotificationEndPoints.EXPO_STATUS_CHANGED, command);

        if (!res.getStatusCode().is2xxSuccessful()) {
            throw new CustomException( CustomErrorCode.NOTIFICATION_SAVE_FAILED);
        }

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

        ResponseEntity<Void> res = notificationInternalClient.send(NotificationEndPoints.EVENT_REMINDER, body);

        if (!res.getStatusCode().is2xxSuccessful()) {
            throw new CustomException( CustomErrorCode.NOTIFICATION_SAVE_FAILED);
        }

    }

    public void notifyAdStatusChange(Advertisement ad, AdvertisementStatus oldStatus, AdvertisementStatus newStatus) {

        AdStatusChangeCommand command = commandGenerator(ad, oldStatus, newStatus);
        ResponseEntity<Void> res = notificationInternalClient.send(NotificationEndPoints.AD_STATUS_CHANGED, command);

        if (!res.getStatusCode().is2xxSuccessful()) {
            throw new CustomException( CustomErrorCode.NOTIFICATION_SAVE_FAILED);
        }

    }

    private AdStatusChangeCommand commandGenerator(Advertisement ad, AdvertisementStatus oldStatus, AdvertisementStatus newStatus){

        return AdStatusChangeCommand.builder()
                .memberId(ad.getMember().getId())
                .adId(ad.getId())
                .adTitle(ad.getTitle())
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .build();
    }
}
