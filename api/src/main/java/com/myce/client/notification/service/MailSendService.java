package com.myce.client.notification.service;

import com.myce.client.notification.dto.NotificationEndPoints;
import com.myce.reservation.entity.code.UserType;
import com.myce.client.notification.NotificationInternalClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class MailSendService {

    private final NotificationInternalClient notificationInternalClient;

    public void sendMail(String to, String subject, String content
    ) {
        Map<String, Object> body = Map.of(
                "to", to,
                "subject", subject,
                "content", content
        );
        notificationInternalClient.send(NotificationEndPoints.MAIL_SEND,body);

    }
    public void sendMailToMultiple(List<String> recipients, String subject, String content) {

        Map<String, Object> body = Map.of(
                "recipients", recipients,
                "subject", subject,
                "content", content

        );
        notificationInternalClient.send(NotificationEndPoints.MULTI_MAIL_SEND,body);
    }
    public void sendSupportMail(String to, String subject, String content
    ) {
        Map<String, Object> body = Map.of(
                "to", to,
                "subject", subject,
                "content", content
        );
        notificationInternalClient.send( NotificationEndPoints.SUPPORT_MAIL_SEND,body);

    }

    public void sendVerificationMail(String email, String verificationName, String code, String limitTime) {
        Map<String, Object> body = Map.of(
                "email", email,
                "verificationName", verificationName,
                "code", code,
                "limitTime", limitTime
        );
        notificationInternalClient.send(NotificationEndPoints.VERIFICATION_MAIL_SEND,body);

    }

    public void sendResetPwMail(String email,String password
    ) {
        Map<String, Object> body = Map.of(
                "email", email,
                "password", password
        );
        notificationInternalClient.send(NotificationEndPoints.RESET_MAIL_SEND,body);

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
        notificationInternalClient.send( NotificationEndPoints.RESERVATION_CONFIRM_MAIL_SEND,body);

    }
}
