package com.myce.system.entity.type;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum StandardType {
    AFTER_RESERVATION("예매 후"),
    BEFORE_EXPO_START("관람 전");

    private final String description;

    public static StandardType fromString(String value) {
        for (StandardType type : StandardType.values()) {
            if (type.name().equals(value)) {
                return type;
            }
        }

        throw new IllegalArgumentException("NOT_EXIST_REFUND_TYPE: " + value);    }
}
