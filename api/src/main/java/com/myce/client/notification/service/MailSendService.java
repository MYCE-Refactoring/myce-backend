package com.myce.client.notification.service;

import com.myce.client.notification.dto.NotificationEndPoints;
import com.myce.reservation.entity.code.UserType;
import com.myce.client.notification.NotificationClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class MailSendService {

    private final NotificationClient notificationClient;

    public void sendMail(String to, String subject, String content
    ) {
        Map<String, Object> body = Map.of(
                "to", to,
                "subject", subject,
                "content", content
        );
        notificationClient.send(NotificationEndPoints.MAIL_SEND,body);

    }
    public void sendMailToMultiple(List<String> recipients, String subject, String content) {

        Map<String, Object> body = Map.of(
                "recipients", recipients,
                "subject", subject,
                "content", content

        );
        notificationClient.send(NotificationEndPoints.MULTI_MAIL_SEND,body);
    }
    public void sendSupportMail(String to, String subject, String content
    ) {
        Map<String, Object> body = Map.of(
                "to", to,
                "subject", subject,
                "content", content
        );
        notificationClient.send( NotificationEndPoints.SUPPORT_MAIL_SEND,body);

    }

    public void sendVerificationMail(String email, String verificationName, String code, String limitTime) {
        Map<String, Object> body = Map.of(
                "email", email,
                "verificationName", verificationName,
                "code", code,
                "limitTime", limitTime
        );
        notificationClient.send(NotificationEndPoints.VERIFICATION_MAIL_SEND,body);

    }

    public void sendResetPwMail(String email,String password
    ) {
        Map<String, Object> body = Map.of(
                "email", email,
                "password", password
        );
        notificationClient.send(NotificationEndPoints.RESET_MAIL_SEND,body);

    }

    public void sendConfirmMail(String email, String name, String expoTitle, String reservationCode,
                                Integer quantity, String paymentAmount, UserType userType
    ) {
        Map<String, Object> body = Map.of(
                "email", email,
                "name", name,
                "expoTitle", expoTitle,
                "reservationCode", reservationCode,
                "quantity", quantity,
                "paymentAmount", paymentAmount,
                "userType", userType
        );
        notificationClient.send( NotificationEndPoints.RESERVATION_CONFIRM_MAIL_SEND,body);

    }
}
