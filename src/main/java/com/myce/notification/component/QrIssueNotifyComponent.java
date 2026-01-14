package com.myce.notification.component;

import com.myce.restclient.service.NotificationClientService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class QrIssueNotifyComponent {

    private final NotificationClientService notificationClientService;

    public void sendQrIssuedNotification(Long memberId,
                            Long reservationId,
                            String expoTitle) {

        Map<String, Object> body = Map.of(
                "memberId", memberId,
                "reservationId", reservationId,
                "expoTitle", expoTitle,
                "reissue", false
        );
        notificationClientService.send( "notifications/qr-issued", body);
    }
}
