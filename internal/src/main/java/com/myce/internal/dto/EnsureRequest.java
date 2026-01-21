package com.myce.internal.dto;

import com.myce.internal.dto.type.ExpoAdminPermission;
import com.myce.internal.dto.type.LoginType;
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
