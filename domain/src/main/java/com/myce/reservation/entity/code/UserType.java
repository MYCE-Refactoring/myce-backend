package com.myce.reservation.entity.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserType {
    MEMBER("회원"),
    GUEST("비회원");

    private final String label;

    public static UserType fromValue(String value) {
        for (UserType type : UserType.values()) {
            if (type.name().equalsIgnoreCase(value)) return type;
        }
        throw new IllegalArgumentException("USER_TYPE_INVALID: " + value);
    }

    public static UserType fromLabel(String label) {
        for (UserType type : UserType.values()) {
            if (type.getLabel().equals(label)) return type;
        }
        throw new IllegalArgumentException("USER_TYPE_INVALID: " + label);    }
}
