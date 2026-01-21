package com.myce.expo.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;

@Getter
public class ExpoInfoListResponse {
    List<ExpoInfoResponse> expoInfos;

    public ExpoInfoListResponse() {
        expoInfos = new ArrayList<>();
    }

    public void addExpoInfo(ExpoInfoResponse expoInfo) {
        this.expoInfos.add(expoInfo);
    }
}
