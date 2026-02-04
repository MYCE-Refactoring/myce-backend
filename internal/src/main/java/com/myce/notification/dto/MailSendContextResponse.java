package com.myce.notification.dto;

import com.myce.reservation.dto.RecipientInfoDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MailSendContextResponse {
    String expoName;
    String contactPhone;
    String contactEmail;
    List<RecipientInfoDto> recipientInfos;
}
