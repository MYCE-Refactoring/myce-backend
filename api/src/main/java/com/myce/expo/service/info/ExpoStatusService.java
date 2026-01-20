package com.myce.expo.service.info;

import com.myce.expo.entity.Expo;
import com.myce.expo.entity.type.ExpoStatus;

public interface ExpoStatusService {
    void verifyCancelExpo(Expo expo);
    void verifyApproveExpo(Expo expo);
    void verifyRejectExpo(Expo expo);
    void verifyApproveCancellation(Expo expo);
    void verifyPublish(Expo expo);
    void verifyComplete(Expo expo);
    void verifyApproveSettlement(Expo expo);
    void verifyCancelByStatus(Expo expo);
}
