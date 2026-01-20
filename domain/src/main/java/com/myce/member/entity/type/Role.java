package com.myce.member.entity.type;


public enum Role {
    PLATFORM_ADMIN, USER, EXPO_ADMIN;

    public static Role fromName(String name) {
        for(Role role : Role.values()) {
            if(role.name().equals(name)) {
                return role;
            }
        }

        throw new IllegalArgumentException("MEMBER_ROLE_NOT_EXIST: " + name);
    }
}
