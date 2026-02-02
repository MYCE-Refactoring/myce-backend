package com.myce.advertisement.service.impl;

import com.myce.advertisement.dto.AdCancelInfoCheck;
import com.myce.advertisement.entity.Advertisement;
import com.myce.advertisement.repository.AdRepository;
import com.myce.client.payment.service.PaymentInternalService;
import com.myce.client.payment.service.RefundInternalService;
import com.myce.advertisement.service.AdPlatformCurrentService;
import com.myce.advertisement.service.mapper.AdInfoMapper;
import com.myce.common.exception.CustomErrorCode;
import com.myce.common.exception.CustomException;
import com.myce.payment.dto.PaymentRefundRequest;
import com.myce.payment.dto.RefundInternalRequest;
import com.myce.payment.dto.RefundInternalResponse;
import com.myce.payment.dto.PaymentInternalDetailResponse;
import com.myce.payment.entity.type.PaymentTargetType;
import com.myce.payment.entity.type.RefundStatus;
import com.myce.payment.service.refund.PaymentRefundService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
@Slf4j
public class AdPlatformCurrentServiceImpl implements AdPlatformCurrentService {
    private final AdRepository adRepository;
    private final RefundInternalService refundInternalService;
    private final PaymentRefundService paymentRefundService;
    private final AdStatusValidateComponent adStatusValidateComponent;

    private final PaymentInternalService paymentInternalService;

    public AdCancelInfoCheck generateCancelCheck(Long adId) {
        log.info("generateCancelCheck - Advertisement Id : {}, targetType : {}", adId, PaymentTargetType.AD.name());
        Advertisement ad = adRepository
                .findById(adId)
                .orElseThrow(() -> new CustomException(CustomErrorCode.AD_NOT_FOUND));
        PaymentInternalDetailResponse payment =
                paymentInternalService.getPaymentByTarget(PaymentTargetType.AD, ad.getId());
        if (payment == null) {
            throw new CustomException(CustomErrorCode.PAYMENT_INFO_NOT_FOUND);
        }
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

        // 상태 검증
        adStatusValidateComponent.verifyDenyCancel(ad);

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
        adStatusValidateComponent.verifyCancel(ad);

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
