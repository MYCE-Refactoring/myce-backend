package com.myce.advertisement.service.impl;

import com.myce.advertisement.entity.Advertisement;
import com.myce.advertisement.entity.type.AdvertisementStatus;
import com.myce.advertisement.service.AdStatusService;
import com.myce.common.exception.CustomErrorCode;
import com.myce.common.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdStatusServiceImpl implements AdStatusService {

    @Override
    public void verifyApprove(Advertisement ad) {
        AdvertisementStatus status = ad.getStatus();
        if(status != AdvertisementStatus.PENDING_APPROVAL){
            throw new CustomException( CustomErrorCode.INVALID_ADVERTISEMENT_STATUS);
        }
    }

    @Override
    public void verifyCancel(Advertisement ad) {
        AdvertisementStatus status = ad.getStatus();
        if (status != AdvertisementStatus.PUBLISHED &&
                status != AdvertisementStatus.PENDING_CANCEL) {
            throw new CustomException(CustomErrorCode.INVALID_ADVERTISEMENT_STATUS);
        }
    }

    @Override
    public void verifyReject(Advertisement ad) {
        AdvertisementStatus status = ad.getStatus();
        if (status != AdvertisementStatus.PENDING_APPROVAL &&
                status != AdvertisementStatus.PENDING_PAYMENT) {
            throw new CustomException(CustomErrorCode.INVALID_ADVERTISEMENT_STATUS);
        }
    }

    @Override
    public void verifyPublish(Advertisement ad) {
        AdvertisementStatus status = ad.getStatus();
        if (status != AdvertisementStatus.PENDING_PUBLISH) {
            throw new CustomException(CustomErrorCode.INVALID_ADVERTISEMENT_STATUS);
        }
    }

    @Override
    public void verifyDenyCancel(Advertisement ad) {
        AdvertisementStatus status = ad.getStatus();

        if (status != AdvertisementStatus.PENDING_CANCEL) {
            throw new CustomException(CustomErrorCode.INVALID_ADVERTISEMENT_STATUS);
        }
    }
    @Override
    public void verifyComplete(Advertisement ad) {
        AdvertisementStatus status = ad.getStatus();

        if (status != AdvertisementStatus.PUBLISHED) {
            throw new CustomException(CustomErrorCode.INVALID_ADVERTISEMENT_STATUS);
        }
    }

    // 상태별 취소 처리
    @Override
    public void verifyCancelByStatus(Advertisement ad) {
        AdvertisementStatus status = ad.getStatus();
        if (status == AdvertisementStatus.PENDING_APPROVAL ||
                status == AdvertisementStatus.PENDING_PAYMENT ||
                status == AdvertisementStatus.PUBLISHED ||
                status == AdvertisementStatus.PENDING_CANCEL) {
            return;
        }
        throw new CustomException(CustomErrorCode.INVALID_ADVERTISEMENT_STATUS);
    }

    // 상태별 환불 신청 처리
    @Override
    public void verifyRequestRefundByStatus(Advertisement ad) {
        AdvertisementStatus status = ad.getStatus();
        if (status == AdvertisementStatus.PUBLISHED ||
                status == AdvertisementStatus.PENDING_PUBLISH) {
            return;
        }
        throw new CustomException(CustomErrorCode.INVALID_ADVERTISEMENT_STATUS);
    }

    @Override
    public void verifyCancelPendingApproval(Advertisement ad) {
        AdvertisementStatus status = ad.getStatus();
        if (status != AdvertisementStatus.PENDING_APPROVAL) {
            throw new CustomException(CustomErrorCode.INVALID_ADVERTISEMENT_STATUS);
        }
    }

    @Override
    public void verifyCancelPendingPayment(Advertisement ad ) {
        AdvertisementStatus status = ad.getStatus();
        if (status != AdvertisementStatus.PENDING_PAYMENT) {
            throw new CustomException(CustomErrorCode.INVALID_ADVERTISEMENT_STATUS);
        }
    }
    @Override
    public void verifyRequestRefund(Advertisement ad) {
        AdvertisementStatus status = ad.getStatus();

        if (status != AdvertisementStatus.PENDING_PUBLISH) {
            throw new CustomException(CustomErrorCode.INVALID_ADVERTISEMENT_STATUS);
        }
    }

    @Override
    public void verifyRequestPartialRefund(Advertisement ad) {
        AdvertisementStatus status = ad.getStatus();

        if (status != AdvertisementStatus.PUBLISHED) {
            throw new CustomException(CustomErrorCode.INVALID_ADVERTISEMENT_STATUS);
        }
    }
}
