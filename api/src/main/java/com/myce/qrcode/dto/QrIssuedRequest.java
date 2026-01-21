package com.myce.qrcode.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class QrIssuedRequest {
    private Long memberId;
    private Long reservationId;
    private String expoTitle;
    private boolean isReissue;
}