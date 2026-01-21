package com.myce.notification.internal.dto;

import com.myce.auth.dto.type.LoginType;
import com.myce.common.permission.ExpoAdminPermission;
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
