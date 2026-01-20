package com.myce.member.entity.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Gender {
    MALE("남"),
    FEMALE("여");

    private final String label;

    public static Gender fromString(String value) {
        for (Gender genderEnum : Gender.values()) {
            if (genderEnum.toString().equals(value)) {
                return genderEnum;
            }
        }
        throw new IllegalArgumentException("GENDER_TYPE_INVALID: " + value);
    }

    public static Gender fromLabel(String label){
        for (Gender genderEnum : Gender.values()) {
            if(genderEnum.getLabel().equals(label)){
                return genderEnum;
            }
        }
        throw new IllegalArgumentException("GENDER_TYPE_INVALID: " + label);
    }

    public static String toLabel(Gender gender){
        if(gender==null){
            throw new IllegalArgumentException("GENDER_TYPE_INVALID: ");
        }
        return gender.getLabel();
    }
}