package com.myce.notification.component;

import com.myce.notification.component.endpoints.NotificationEndPoints;
import com.myce.restclient.service.NotificationClientService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class MemberNotificationComponent {

    private final NotificationClientService notificationClientService;

    public void sendMemberNotification(Long memberId, Long reservationId,
                                       String expoTitle, boolean isReissue) {

        Map<String, Object> body = Map.of(
                "memberId", memberId,
                "reservationId", reservationId,
                "expoTitle", expoTitle,
                "reissue", isReissue
        );
        notificationClientService.send(NotificationEndPoints.QR_ISSUED, body);
    }

}
