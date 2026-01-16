package com.myce.notification.component;

import com.myce.notification.component.endpoints.NotificationEndPoints;
import com.myce.restclient.dto.PaymentCompleteRequest;
import com.myce.restclient.service.NotificationClientService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentCompleteComponent {

    private final NotificationClientService notificationClientService;

    public void sendPaymentComplete(PaymentCompleteRequest req) {
        notificationClientService.send( NotificationEndPoints.PAYMENT_COMPLETED, req );
    }
}