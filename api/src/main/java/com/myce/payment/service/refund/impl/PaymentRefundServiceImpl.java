package com.myce.payment.service.refund.impl;

import com.myce.advertisement.repository.AdRepository;
import com.myce.client.notification.service.NotificationService;
import com.myce.client.payment.service.RefundInternalService;
import com.myce.common.exception.CustomErrorCode;
import com.myce.common.exception.CustomException;
import com.myce.payment.dto.*;
import com.myce.payment.entity.AdPaymentInfo;
import com.myce.payment.entity.ExpoPaymentInfo;
import com.myce.payment.entity.Payment;
import com.myce.payment.entity.Refund;
import com.myce.payment.entity.ReservationPaymentInfo;
import com.myce.payment.entity.type.PaymentStatus;
import com.myce.payment.entity.type.PaymentTargetType;
import com.myce.payment.entity.type.RefundStatus;
import com.myce.payment.repository.AdPaymentInfoRepository;
import com.myce.payment.repository.ExpoPaymentInfoRepository;
import com.myce.payment.repository.PaymentRepository;
import com.myce.payment.repository.RefundRepository;
import com.myce.payment.repository.ReservationPaymentInfoRepository;
import com.myce.advertisement.entity.Advertisement;
import com.myce.advertisement.entity.type.AdvertisementStatus;
import com.myce.payment.service.portone.PortOneApiService;
import com.myce.payment.service.refund.PaymentRefundService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentRefundServiceImpl implements PaymentRefundService {

    // 핵심: payment internal API 호출자
    private final RefundInternalService refundInternalService;
    private final AdPaymentInfoRepository adPaymentInfoRepository;
    private final ExpoPaymentInfoRepository expoPaymentInfoRepository;
    private final ReservationPaymentInfoRepository reservationPaymentInfoRepository;
    private final AdRepository advertisementRepository;

    private final NotificationService notificationService;

    @Override
    @Transactional
    public RefundInternalResponse refundPayment(PaymentRefundRequest request) {
        // 1) public request → internal request로 변환
        RefundInternalRequest internalRequest = RefundInternalRequest.builder()
                .impUid(request.getImpUid())
                .merchantUid(request.getMerchantUid())
                .cancelAmount(request.getCancelAmount())
                .reason(request.getReason())
                .refundHolder(request.getRefundHolder())
                .refundBank(request.getRefundBank())
                .refundAccount(request.getRefundAccount())
                .refundTel(request.getRefundTel())
                .build();

        // 2) payment internal에 환불 처리 위임 (PortOne 호출/Refund 저장은 payment에서)
        RefundInternalResponse response = refundInternalService.refund(internalRequest);

        // 3) core는 PaymentInfo 상태만 업데이트
        updatePaymentInfoStatus(response);

        return response;
    }

    @Override
    public String getImpUidForRefund(PaymentImpUidForRefundRequest request) {
        // core는 payment DB를 직접 안 봄 → internal 조회로 대체
        return refundInternalService.getImpUid(request.getTargetType(), request.getTargetId());
    }

    @Override
    @Transactional
    public RefundInternalResponse processAdRefund(AdRefundRequest request) {
        // 1) 광고 조회 (상태 변경/알림은 core 책임)
        Advertisement advertisement = advertisementRepository.findById(request.getAdId())
                .orElseThrow(() -> new CustomException(CustomErrorCode.AD_NOT_FOUND));
        // 2) adId → impUid 조회는 internal 조회로 처리
        String impUid = refundInternalService.getImpUid(PaymentTargetType.AD, request.getAdId());

        // 3) internal 환불 요청 구성
        RefundInternalRequest internalRequest = RefundInternalRequest.builder()
                .impUid(impUid)
                .cancelAmount(request.getCancelAmount())
                .reason(request.getReason())
                .build();

        // 4) payment internal에 환불 위임
        RefundInternalResponse response = refundInternalService.refund(internalRequest);

        // 5) 광고 상태 변경 + 알림 전송 (core 역할 유지)
        AdvertisementStatus oldStatus = advertisement.getStatus();
        advertisement.updateStatus(AdvertisementStatus.CANCELLED);
        AdvertisementStatus newStatus = AdvertisementStatus.CANCELLED;
        advertisementRepository.save(advertisement);

        notificationService.notifyAdStatusChange(advertisement, oldStatus, newStatus);

        updatePaymentInfoStatus(response);

        return response;

    }

    // 결제 상세 상태 업데이트
    private void updatePaymentInfoStatus(RefundInternalResponse response) {
        if (response == null || response.getTargetType() == null || response.getTargetId() == null) {
            throw new CustomException(CustomErrorCode.INTERNAL_SERVER_ERROR);
        }
        boolean isPartial = Boolean.TRUE.equals(response.getIsPartial());
        PaymentStatus newStatus = isPartial ? PaymentStatus.PARTIAL_REFUNDED : PaymentStatus.REFUNDED;

        switch (response.getTargetType()) {
            case RESERVATION:
                ReservationPaymentInfo rpi = reservationPaymentInfoRepository.findByReservationId(response.getTargetId() ).orElseThrow( () -> new CustomException( CustomErrorCode.PAYMENT_INFO_NOT_FOUND ) );
                rpi.setStatus( newStatus );
                reservationPaymentInfoRepository.save(rpi);
                break;
            case AD:
                AdPaymentInfo api = adPaymentInfoRepository.findByAdvertisementId(response.getTargetId()).orElseThrow( () -> new CustomException( CustomErrorCode.PAYMENT_INFO_NOT_FOUND ) );
                api.setStatus(newStatus);
                adPaymentInfoRepository.save( api );
                break;
            case EXPO:
                ExpoPaymentInfo epi = expoPaymentInfoRepository.findByExpoId(response.getTargetId()).orElseThrow( () -> new CustomException( CustomErrorCode.PAYMENT_INFO_NOT_FOUND ) );
                epi.setStatus( newStatus );
                expoPaymentInfoRepository.save( epi );
                break;
            default:
                throw new CustomException( CustomErrorCode.INVALID_PAYMENT_TARGET_TYPE );
        }
    }
}
