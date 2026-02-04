package com.myce.expo.service.info.impl;

import com.myce.common.exception.CustomErrorCode;
import com.myce.common.exception.CustomException;
import com.myce.expo.entity.Expo;
import com.myce.expo.entity.type.ExpoStatus;
import com.myce.expo.service.info.ExpoStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ExpoStatusServiceImpl implements ExpoStatusService {

    @Override
    public void verifyCancelExpo(Expo expo) {
        ExpoStatus status = expo.getStatus();
        if (status != ExpoStatus.PENDING_APPROVAL &&
                status != ExpoStatus.PENDING_PAYMENT) {
            throw new CustomException(CustomErrorCode.INVALID_EXPO_STATUS);
        }
    }
    @Override
    public void verifyApproveExpo(Expo expo) {
        ExpoStatus status = expo.getStatus();
        if (status != ExpoStatus.PENDING_APPROVAL) {
            throw new CustomException(CustomErrorCode.INVALID_EXPO_STATUS);
        }
    }

    @Override
    public void verifyRejectExpo(Expo expo) {
        ExpoStatus status = expo.getStatus();
        if (status != ExpoStatus.PENDING_APPROVAL) {
            throw new CustomException(CustomErrorCode.INVALID_EXPO_STATUS);
        }
    }

    @Override
    public void verifyApproveCancellation(Expo expo) {
        ExpoStatus status = expo.getStatus();

        if (status != ExpoStatus.PENDING_CANCEL) {
            throw new CustomException(CustomErrorCode.INVALID_EXPO_STATUS);
        }
    }

    /**
     * 박람회 자동 게시 처리 (스케줄러용)
     * PENDING_PUBLISH -> PUBLISHED
     */
    @Override
    public void verifyPublish(Expo expo) {
        ExpoStatus status = expo.getStatus();

        if (status != ExpoStatus.PENDING_PUBLISH) {
            throw new CustomException(CustomErrorCode.INVALID_EXPO_STATUS);
        }
    }

    /**
     * 박람회 자동 게시 종료 처리 (스케줄러용)
     * PUBLISHED -> PUBLISH_ENDED
     */
    @Override
    public void verifyComplete(Expo expo) {
        ExpoStatus status = expo.getStatus();

        if (status != ExpoStatus.PUBLISHED) {
            throw new CustomException(CustomErrorCode.INVALID_EXPO_STATUS);
        }
    }

    /**
     * 박람회 정산 승인 처리 (플랫폼 관리자용)
     * SETTLEMENT_REQUESTED -> COMPLETED
     */
    @Override
    public void verifyApproveSettlement(Expo expo) {
        ExpoStatus status = expo.getStatus();

        if (status != ExpoStatus.SETTLEMENT_REQUESTED) {
            throw new CustomException(CustomErrorCode.INVALID_EXPO_STATUS);
        }
    }

    // 상태별 취소 처리
    @Override
    public void verifyCancelByStatus(Expo expo) {
        ExpoStatus status = expo.getStatus();

        switch (status) {
            case PENDING_APPROVAL:
            case PENDING_PAYMENT:
                break;
            default:
                throw new CustomException(CustomErrorCode.INVALID_EXPO_STATUS);
        }
    }

}
