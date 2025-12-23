package com.myce.expo.service.admin;

import com.myce.member.dto.expo.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ExpoLifeCycleService {
    
    Page<MemberExpoResponse> getMemberExpos(Long memberId, Pageable pageable);
    
    MemberExpoDetailResponse getMemberExpoDetail(Long memberId, Long expoId);
    
    void cancelExpo(Long memberId, Long expoId);
    
    ExpoPaymentDetailResponse getExpoPaymentDetail(Long memberId, Long expoId);
    
    List<ExpoAdminCodeResponse> getExpoAdminCodes(Long memberId, Long expoId);
    
    ExpoSettlementReceiptResponse getExpoSettlementReceipt(Long memberId, Long expoId);
    
    ExpoRefundReceiptResponse getExpoRefundReceipt(Long memberId, Long expoId);
    
    ExpoRefundReceiptResponse getExpoRefundHistory(Long memberId, Long expoId);
    
    void requestExpoSettlement(Long memberId, Long expoId, ExpoSettlementRequest request);
    
    void completeExpoPayment(Long memberId, Long expoId);
}