package com.myce.payment.service.webhook.impl;

import com.myce.client.payment.service.PaymentInternalService;
import com.myce.common.exception.CustomErrorCode;
import com.myce.common.exception.CustomException;
import com.myce.payment.dto.PaymentWebhookInternalRequest;
import com.myce.payment.dto.PaymentWebhookInternalResponse;
import com.myce.payment.dto.PortOneWebhookRequest;
import com.myce.payment.entity.AdPaymentInfo;
import com.myce.payment.entity.ExpoPaymentInfo;
import com.myce.payment.entity.ReservationPaymentInfo;
import com.myce.payment.entity.type.PaymentStatus;
import com.myce.payment.repository.AdPaymentInfoRepository;
import com.myce.payment.repository.ExpoPaymentInfoRepository;
import com.myce.payment.repository.ReservationPaymentInfoRepository;
import com.myce.payment.service.impl.PaymentCommonService;
import com.myce.payment.service.webhook.PaymentWebhookService;
import com.myce.reservation.entity.Reservation;
import com.myce.reservation.entity.code.ReservationStatus;
import com.myce.reservation.entity.code.UserType;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentWebhookServiceImpl implements PaymentWebhookService {

    private static final String PORTONE_STATUS_PAID = "paid";
    // payment 내부 API 호출용 (PortOne 재조회 및 paidAt 갱신은 payment 서비스에서 수행)
    private final PaymentInternalService paymentInternalService;
    private final AdPaymentInfoRepository adPaymentInfoRepository;
    private final ExpoPaymentInfoRepository expoPaymentInfoRepository;
    private final ReservationPaymentInfoRepository reservationPaymentInfoRepository;
    private final PaymentCommonService paymentCommonService;

    // 가상계좌 입금 처리 웹훅
    @Override
    @Transactional
    public void processWebhook(PortOneWebhookRequest request) {
        // 0) ready 등 paid가 아닌 상태는 무시 (PortOne 재조회 불필요)
        String requestStatus = request.getStatus();
        if (requestStatus == null || !PORTONE_STATUS_PAID.equalsIgnoreCase(requestStatus)) {
            log.info("[웹훅 무시] 포트원 상태가 paid 아님. status={}", requestStatus);
            return;
        }

        // 1) PortOne 조회/paidAt 갱신은 payment 내부로 위임 (core는 도메인 상태만 갱신)
        PaymentWebhookInternalResponse internalResponse = paymentInternalService.processWebhook(
                PaymentWebhookInternalRequest.builder()
                        .impUid(request.getImpUid())
                        .merchantUid(request.getMerchantUid())
                        .build()
        );

        // 2) paid가 아니면 도메인 업데이트를 하지 않음 (ready 등은 무시)
        String status = internalResponse.getStatus();
        if (status == null || !PORTONE_STATUS_PAID.equalsIgnoreCase(status)) {
            log.info("[웹훅 무시] 포트원 상태가 paid 아님. status={}", status);
            return;
        }

        // 3) paid 상태일 때만 targetType/targetId/paidAmount로 도메인 상태 갱신
        if (internalResponse.getTargetType() == null
                || internalResponse.getTargetId() == null
                || internalResponse.getPaidAmount() == null) {
            throw new CustomException(CustomErrorCode.PAYMENT_INFO_NOT_FOUND);
        }

        switch (internalResponse.getTargetType()) {
            case RESERVATION:
                processReservation(internalResponse.getTargetId(), internalResponse.getPaidAmount());
                break;
            case AD:
                processAd(internalResponse.getTargetId(), internalResponse.getPaidAmount());
                break;
            case EXPO:
                processExpo(internalResponse.getTargetId(), internalResponse.getPaidAmount());
                break;
            default:
                throw new CustomException(CustomErrorCode.INVALID_PAYMENT_TARGET_TYPE);
        }

        log.info("[웹훅 처리 완료] imp_uid={}, merchant_uid={}, paid_at={}",
                internalResponse.getImpUid(), internalResponse.getMerchantUid(), internalResponse.getPaidAt());
    }

    /**
     * 예약 결제 성공 처리
     * - 결제 금액 검증
     * - 예약 상태 CONFIRMED
     * - QR 발급 + 마일리지/알림 (회원만)
     */
    private void processReservation(Long paymentTargetId, int paidAmount) {
        ReservationPaymentInfo reservationPaymentInfo = reservationPaymentInfoRepository
                .findByReservationId(paymentTargetId)
                .orElseThrow(() -> new CustomException(CustomErrorCode.PAYMENT_INFO_NOT_FOUND));

        if (isPassStatus(reservationPaymentInfo.getStatus())) return;

        if (!reservationPaymentInfo.getTotalAmount().equals(paidAmount)) {
            throw new CustomException(CustomErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }

        reservationPaymentInfo.setStatus(PaymentStatus.SUCCESS);

        // 가상계좌 입금 확인 시 reservation CONFIRMED로
        Reservation reservation = reservationPaymentInfo.getReservation();
        if(reservation == null) throw new CustomException(CustomErrorCode.RESERVATION_NOT_FOUND);

        reservation.updateStatus(ReservationStatus.CONFIRMED);

        // QR 코드 생성 시도
        paymentCommonService.issueQrForReservation(reservation);

        UserType userType = reservation.getUserType();
        if (userType.equals(UserType.MEMBER)) {
            // 가상계좌 입금 완료 시 마일리지 및 등급 업데이트 (회원만)
            int usedMileage = Objects.requireNonNullElse(reservationPaymentInfo.getUsedMileage(), 0);
            int savedMileage = Objects.requireNonNullElse(reservationPaymentInfo.getSavedMileage(), 0);
            paymentCommonService.processMileage(usedMileage, savedMileage, reservation.getUserId());
            // 결제 완료 알림 발송 (회원만)
            paymentCommonService.sendAlert(reservation, paidAmount);
        }

        reservationPaymentInfoRepository.save(reservationPaymentInfo);
    }

    /**
     * 광고 결제 성공 처리
     * - 결제 금액 검증
     * - 광고 결제 상태 SUCCESS
     */
    private void processAd(Long paymentTargetId, int paidAmount) {
        AdPaymentInfo adPaymentInfo = adPaymentInfoRepository
                .findByAdvertisementId(paymentTargetId)
                .orElseThrow(() -> new CustomException(CustomErrorCode.PAYMENT_INFO_NOT_FOUND));

        if (isPassStatus(adPaymentInfo.getStatus())) return;

        if (!adPaymentInfo.getTotalAmount().equals(paidAmount)) {
            throw new CustomException(CustomErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }

        adPaymentInfo.setStatus(PaymentStatus.SUCCESS);
        adPaymentInfoRepository.save(adPaymentInfo);
    }

    /**
     * 박람회 결제 성공 처리
     * - 결제 금액 검증
     * - 박람회 결제 상태 SUCCESS
     */
    private void processExpo(long targetId, int paidAmount) {
        ExpoPaymentInfo expoPaymentInfo = expoPaymentInfoRepository
                .findByExpoId(targetId)
                .orElseThrow(() -> new CustomException(CustomErrorCode.PAYMENT_INFO_NOT_FOUND));

        if (isPassStatus(expoPaymentInfo.getStatus())) return;

        if (!expoPaymentInfo.getTotalAmount().equals(paidAmount)) {
            throw new CustomException(CustomErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }

        expoPaymentInfo.setStatus(PaymentStatus.SUCCESS);
        expoPaymentInfoRepository.save(expoPaymentInfo);
    }

    /**
     * 중복 처리 방지용 상태 체크
     * - SUCCESS/REFUNDED/PARTIAL_REFUNDED는 재처리 금지
     */
    private boolean isPassStatus(PaymentStatus status) {
        return status.equals(PaymentStatus.SUCCESS)
                || status.equals(PaymentStatus.REFUNDED)
                || status.equals(PaymentStatus.PARTIAL_REFUNDED);
    }
}
