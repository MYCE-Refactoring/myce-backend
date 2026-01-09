package com.myce.advertisement.service.component;

import com.myce.advertisement.entity.Advertisement;
import com.myce.advertisement.entity.type.AdvertisementStatus;
import com.myce.expo.entity.Expo;
import com.myce.notification.dto.AdStatusChangeCommand;
import com.myce.notification.dto.ExpoStatusChangeCommand;
import com.myce.notification.service.NotificationService;
import com.myce.restclient.service.RestClientService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdNotificationComponent {

    private final NotificationService notificationService;
    private final RestClientService restClientService;

    public void notifyAdStatusChange(Advertisement ad, AdvertisementStatus oldStatus, AdvertisementStatus newStatus) {

        AdStatusChangeCommand command = commandGenerator(ad, oldStatus, newStatus);
        restClientService.send("notifications/ad-status-changed", command);
//        notificationService.sendAdvertisementStatusChangeNotification(command);

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
