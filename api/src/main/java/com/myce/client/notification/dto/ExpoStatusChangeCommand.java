package com.myce.client.notification.dto;

import com.myce.expo.entity.type.ExpoStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ExpoStatusChangeCommand {
        Long memberId;
        Long expoId;
        String expoTitle;
        ExpoStatus oldStatus;
        ExpoStatus newStatus;
}
