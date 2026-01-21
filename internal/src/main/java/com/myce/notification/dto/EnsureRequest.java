package com.myce.notification.dto;

import com.myce.notification.dto.type.ExpoAdminPermission;
import com.myce.notification.dto.type.LoginType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class EnsureRequest {

    Long expoId;
    Long memberId;
    LoginType loginType;
    ExpoAdminPermission permission;
}
