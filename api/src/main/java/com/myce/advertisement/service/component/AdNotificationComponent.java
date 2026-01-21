package com.myce.advertisement.service.component;

import com.myce.advertisement.entity.Advertisement;
import com.myce.advertisement.entity.type.AdvertisementStatus;
import com.myce.client.notification.dto.AdStatusChangeCommand;
import com.myce.client.notification.NotificationClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdNotificationComponent {

    private final NotificationClient notificationClient;

    public void notifyAdStatusChange(Advertisement ad, AdvertisementStatus oldStatus, AdvertisementStatus newStatus) {

        AdStatusChangeCommand command = commandGenerator(ad, oldStatus, newStatus);
        notificationClient.send("notifications/ad-status-changed", command);

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
