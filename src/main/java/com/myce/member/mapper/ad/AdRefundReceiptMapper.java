package com.myce.member.mapper.ad;

import com.myce.advertisement.entity.Advertisement;
import com.myce.common.entity.BusinessProfile;
import com.myce.member.dto.ad.AdRefundReceiptResponse;
import com.myce.payment.dto.RefundInternalResponse;
import com.myce.payment.entity.AdPaymentInfo;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class AdRefundReceiptMapper {
    
    public AdRefundReceiptResponse toRefundReceiptDto(Advertisement ad, 
                                                     BusinessProfile businessProfile, 
                                                     AdPaymentInfo adPaymentInfo) {
        
        // 환불 계산
        LocalDate today = LocalDate.now();
        LocalDate displayStartDate = ad.getDisplayStartDate();
        
        // 사용한 일수 계산 (게시 시작일부터 오늘까지)
        int usedDays = (int) java.time.temporal.ChronoUnit.DAYS.between(displayStartDate, today) + 1;
        if (usedDays < 0) usedDays = 0;
        
        // 남은 일수 계산
        int remainingDays = adPaymentInfo.getTotalDay() - usedDays;
        if (remainingDays < 0) remainingDays = 0;
        
        // 금액 계산
        int usedAmount = usedDays * adPaymentInfo.getFeePerDay();
        int refundAmount = remainingDays * adPaymentInfo.getFeePerDay();
        
        return AdRefundReceiptResponse.builder()
                .adTitle(ad.getTitle())
                .applicantName(businessProfile.getCompanyName())
                .displayStartDate(ad.getDisplayStartDate())
                .displayEndDate(ad.getDisplayEndDate())
                .status(ad.getStatus())
                .totalDays(adPaymentInfo.getTotalDay())
                .dailyFee(adPaymentInfo.getFeePerDay())
                .totalAmount(adPaymentInfo.getTotalAmount())
                .refundRequestDate(today)
                .usedDays(usedDays)
                .usedAmount(usedAmount)
                .remainingDays(remainingDays)
                .refundAmount(refundAmount)
                .build();
    }
    
    public AdRefundReceiptResponse toRefundHistoryDto(Advertisement ad,
                                                     BusinessProfile businessProfile,
                                                     AdPaymentInfo adPaymentInfo,
                                                     RefundInternalResponse refund) {
        
        // 실제 환불 내역 기반으로 생성 (내부 응답 기준)
        LocalDate refundDate = refund.getRefundedAt() != null
                ? refund.getRefundedAt().toLocalDate()
                : refund.getRequestedAt().toLocalDate();
        
        // 환불 종류에 따른 사용 일수 및 금액 계산
        int usedDays = 0;
        int usedAmount = 0;
        int remainingDays = adPaymentInfo.getTotalDay();
        
        if (Boolean.TRUE.equals(refund.getIsPartial())) {
            // 부분 환불인 경우: 환불 금액으로 남은 일수 계산 후 사용 일수 도출
            if (adPaymentInfo.getFeePerDay() > 0) {
                remainingDays = refund.getRefundedAmount() / adPaymentInfo.getFeePerDay();
                usedDays = adPaymentInfo.getTotalDay() - remainingDays;
                usedAmount = usedDays * adPaymentInfo.getFeePerDay();
            }
        }
        // 전액 환불인 경우는 기본값(0) 유지
        
        return AdRefundReceiptResponse.builder()
                .adTitle(ad.getTitle())
                .applicantName(businessProfile.getCompanyName())
                .displayStartDate(ad.getDisplayStartDate())
                .displayEndDate(ad.getDisplayEndDate())
                .status(ad.getStatus())
                .totalDays(adPaymentInfo.getTotalDay())
                .dailyFee(adPaymentInfo.getFeePerDay())
                .totalAmount(adPaymentInfo.getTotalAmount())
                .refundRequestDate(refundDate) // 실제 환불 요청일
                .usedDays(usedDays)
                .usedAmount(usedAmount)
                .remainingDays(remainingDays)
                .refundAmount(refund.getRefundedAmount()) // 실제 환불된 금액
                .build();
    }
}
