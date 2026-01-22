package com.myce.advertisement.service.impl;

import com.myce.advertisement.dto.AdCancelInfoCheck;
import com.myce.advertisement.entity.Advertisement;
import com.myce.advertisement.repository.AdRepository;
import com.myce.client.payment.service.RefundInternalService;
import com.myce.advertisement.service.AdStatusService;
import com.myce.advertisement.service.PlatformCurrentAdService;
import com.myce.advertisement.service.mapper.AdInfoMapper;
import com.myce.common.exception.CustomErrorCode;
import com.myce.common.exception.CustomException;
import com.myce.payment.dto.PaymentRefundRequest;
import com.myce.payment.dto.RefundInternalRequest;
import com.myce.payment.dto.RefundInternalResponse;
import com.myce.payment.entity.Payment;
import com.myce.payment.entity.type.PaymentStatus;
import com.myce.payment.entity.type.PaymentTargetType;
import com.myce.payment.entity.type.RefundStatus;
import com.myce.payment.service.refund.PaymentRefundService;
import com.myce.payment.repository.AdPaymentInfoRepository;
import com.myce.payment.repository.PaymentRepository;
import com.myce.payment.repository.RefundRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlatformCurrentAdServiceImpl implements PlatformCurrentAdService {
    private final AdRepository adRepository;
    private final PaymentRepository paymentRepository;
    private final AdStatusService adStatusService;
    private final RefundInternalService refundInternalService;
    private final PaymentRefundService paymentRefundService;

    public AdCancelInfoCheck generateCancelCheck(Long adId) {
        log.info("generateCancelCheck - Advertisement Id : {}, targetType : {}", adId, PaymentTargetType.AD.name());
        Advertisement ad = adRepository
                .findById(adId)
                .orElseThrow(() -> new CustomException(CustomErrorCode.AD_NOT_FOUND));
        Payment payment = paymentRepository
                .findByTargetIdAndTargetType(ad.getId(), PaymentTargetType.AD)
                .orElseThrow(() -> new CustomException(CustomErrorCode.PAYMENT_INFO_NOT_FOUND));
        // 환불 조회는 payment internal로 위임 (조회용)
        RefundInternalResponse refund = refundInternalService.getRefundByTarget(
                PaymentTargetType.AD, adId);
        Integer totalAmount = refund.getRefundedAmount();
        log.info("generateCancelCheck - Advertisement : {}, Payment : {}", ad, payment);
        return AdInfoMapper.getAdCancelInfoCheck(payment, ad, totalAmount);
    }

    @Transactional
    public void denyCancel(Long adId){
        Advertisement ad = adRepository
                .findById(adId)
                .orElseThrow(() -> new CustomException(CustomErrorCode.AD_NOT_FOUND));
        Payment payment = paymentRepository
                .findByTargetIdAndTargetType(ad.getId(), PaymentTargetType.AD)
                .orElseThrow(() -> new CustomException(CustomErrorCode.PAYMENT_INFO_NOT_FOUND));

        // 상태 검증
        adStatusService.verifyDenyCancel(ad);

        ad.denyCancel();

        try {
            RefundInternalResponse refund =
                    refundInternalService.getRefundByTarget(PaymentTargetType.AD, adId);

            // 이미 환불 완료된 건은 거절 불가
            if (refund.getStatus() == RefundStatus.REFUNDED) {
                throw new CustomException(CustomErrorCode.ALREADY_REFUNDED);
            }
        if (refund.getStatus() == RefundStatus.PENDING) {
            RefundInternalRequest request = RefundInternalRequest.builder()
                    .paymentId(refund.getPaymentId())
                    .build();
            refundInternalService.rejectRefund(request);
        }
    } catch (CustomException e) {
        // 환불 신청 자체가 없으면 거절 처리만 하고 종료
        if (e.getErrorCode() != CustomErrorCode.REFUND_NOT_FOUND) {
            throw e;
        }
    }
    }

    @Transactional
    public void cancelCurrent(Long adId) {
        Advertisement ad = adRepository
                .findById(adId)
                .orElseThrow(() -> new CustomException(CustomErrorCode.AD_NOT_FOUND));
        RefundInternalResponse refund =
                refundInternalService.getRefundByTarget(PaymentTargetType.AD, adId);

        if (refund.getStatus() != RefundStatus.PENDING) {
            throw new CustomException(CustomErrorCode.ALREADY_REFUNDED);
        }

        //취소 상태검증
        adStatusService.verifyCancel(ad);

        String impUid = refundInternalService.getImpUid(PaymentTargetType.AD, adId);
        PaymentRefundRequest refundRequest = PaymentRefundRequest.builder()
                .impUid(impUid)
                .cancelAmount(refund.getRefundedAmount())
                .reason(refund.getReason())
                .build();

        paymentRefundService.refundPayment(refundRequest);
        ad.cancel();
    }
}
