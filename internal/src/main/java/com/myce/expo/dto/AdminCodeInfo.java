package com.myce.expo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AdminCodeInfo {
    private String code;
    private Long expoId;
    private boolean isInquiryView;
}
