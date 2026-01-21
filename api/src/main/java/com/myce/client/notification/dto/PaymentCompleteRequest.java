package com.myce.client.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentCompleteRequest {
    Long userId;
    Long reservationId;
    String expoTitle;
    String payAmountMessage;
}
