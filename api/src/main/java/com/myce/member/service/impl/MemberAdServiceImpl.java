package com.myce.member.service.impl;

import com.myce.advertisement.dto.AdRegistrationRequest;
import com.myce.advertisement.dto.AdRejectInfoResponse;
import com.myce.advertisement.service.AdUserService;
import com.myce.member.dto.ad.*;
import com.myce.member.service.MemberAdService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class MemberAdServiceImpl implements MemberAdService {

    private final AdUserService adUserService;

    @Override
    public void saveAdvertisement(Long memberId, AdRegistrationRequest request) {
        adUserService.saveAdvertisement(memberId, request);
    }

    @Override
    public Page<MemberAdvertisementResponse> getMemberAdvertisements(Long memberId, Pageable pageable) {
        return adUserService.getMemberAdvertisements(memberId, pageable);
    }

    @Override
    public AdvertisementDetailResponse getAdvertisementDetail(Long memberId, Long advertisementId) {
        return adUserService.getAdvertisementDetail(memberId, advertisementId);
    }

    @Override
    public void cancelAdvertisement(Long memberId, Long advertisementId) {
        adUserService.cancelAdvertisement(memberId, advertisementId);
    }

    @Override
    public void cancelByStatus(Long memberId, Long advertisementId) {
        adUserService.cancelByStatus(memberId, advertisementId);
    }

    @Override
    public void requestRefundByStatus(Long memberId, Long advertisementId, AdRefundRequest request) {
        adUserService.requestRefundByStatus(memberId, advertisementId, request);
    }

    @Override
    public AdvertisementPaymentDetailResponse getAdvertisementPaymentDetail(Long memberId, Long advertisementId) {
        return adUserService.getAdvertisementPaymentDetail(memberId, advertisementId);
    }

    @Override
    public AdvertisementRefundReceiptResponse getAdvertisementRefundReceipt(Long memberId, Long advertisementId) {
        return adUserService.getAdvertisementRefundReceipt(memberId, advertisementId);
    }

    @Override
    public AdRejectInfoResponse getAdvertisementRejectInfo(Long memberId, Long advertisementId) {
        return adUserService.getAdvertisementRejectInfo(memberId, advertisementId);
    }

    @Override
    public AdRefundReceiptResponse getAdvertisementRefundHistory(Long memberId, Long advertisementId) {
        return adUserService.getAdvertisementRefundHistory(memberId, advertisementId);
    }

    @Override
    public void completeAdvertisementPayment(Long memberId, Long advertisementId) {
        adUserService.completeAdvertisementPayment(memberId, advertisementId);
    }
}
