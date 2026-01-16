package com.myce.notification.internal.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class MailSendContextRequest {
    Long expoId;
    String entranceStatus;
    String name;
    String phone;
    String reservationCode;
    String ticketName;
}
