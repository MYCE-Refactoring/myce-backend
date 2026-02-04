package com.myce.expo.entity.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TicketType {
    EARLY_BIRD("얼리버드"),
    GENERAL("일반");

    private final String label;

    public static TicketType fromValue(String type) {
        for (TicketType t : TicketType.values()) {
            if (t.name().equalsIgnoreCase(type)) return t;
        }
        throw new IllegalArgumentException("TICKET_TYPE_INVALID: " + type);
    }

    public static TicketType fromLabel(String label) {
        for (TicketType t : TicketType.values()) {
            if (t.getLabel().equals(label)) return t;
        }
        throw new IllegalArgumentException("TICKET_TYPE_INVALID: " + label);
    }
}