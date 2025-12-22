package com.myce.member.service.impl;

import com.myce.expo.service.ExpoLifeCycleService;
import com.myce.member.dto.expo.*;
import com.myce.member.service.MemberExpoService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MemberExpoServiceImpl implements MemberExpoService {

    private final ExpoLifeCycleService expoLifeCycleService;

    @Override
    public Page<MemberExpoResponse> getMemberExpos(Long memberId, Pageable pageable) {
        return expoLifeCycleService.getMemberExpos(memberId, pageable);
    }


    @Override
    public MemberExpoDetailResponse getMemberExpoDetail(Long memberId, Long expoId) {
        return expoLifeCycleService.getMemberExpoDetail(memberId, expoId);
    }

    @Override
    public void cancelExpo(Long memberId, Long expoId) {
        expoLifeCycleService.cancelExpo(memberId, expoId);
    }

    @Override
    public ExpoPaymentDetailResponse getExpoPaymentDetail(Long memberId, Long expoId) {
        return expoLifeCycleService.getExpoPaymentDetail(memberId, expoId);
    }

    @Override
    public List<ExpoAdminCodeResponse> getExpoAdminCodes(Long memberId, Long expoId) {
        return expoLifeCycleService.getExpoAdminCodes(memberId, expoId);
    }

    @Override
    public ExpoSettlementReceiptResponse getExpoSettlementReceipt(Long memberId, Long expoId) {
        return expoLifeCycleService.getExpoSettlementReceipt(memberId, expoId);
    }

    @Override
    public ExpoRefundReceiptResponse getExpoRefundReceipt(Long memberId, Long expoId) {
        return expoLifeCycleService.getExpoRefundReceipt(memberId, expoId);
    }

    @Override
    public ExpoRefundReceiptResponse getExpoRefundHistory(Long memberId, Long expoId) {
        return expoLifeCycleService.getExpoRefundHistory(memberId, expoId);
    }

    @Override
    public void requestExpoSettlement(Long memberId, Long expoId, ExpoSettlementRequest request) {
        expoLifeCycleService.requestExpoSettlement(memberId, expoId, request);
    }


    @Override
    public void completeExpoPayment(Long memberId, Long expoId) {
        expoLifeCycleService.completeExpoPayment(memberId, expoId);
    }

}
