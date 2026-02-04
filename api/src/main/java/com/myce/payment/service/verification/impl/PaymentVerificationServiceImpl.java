package com.myce.payment.service.verification.impl;

import com.myce.auth.dto.CustomUserDetails;
import com.myce.common.exception.CustomErrorCode;
import com.myce.common.exception.CustomException;
import com.myce.member.service.MemberAdService;
import com.myce.member.service.MemberExpoService;
import com.myce.client.payment.service.PaymentInternalService;
import com.myce.payment.dto.*;
import com.myce.payment.entity.AdPaymentInfo;
import com.myce.payment.entity.ExpoPaymentInfo;
import com.myce.payment.entity.ReservationPaymentInfo;
import com.myce.payment.entity.type.PaymentStatus;
import com.myce.payment.entity.type.PaymentTargetType;
import com.myce.payment.repository.AdPaymentInfoRepository;
import com.myce.payment.repository.ExpoPaymentInfoRepository;
import com.myce.payment.repository.ReservationPaymentInfoRepository;
import com.myce.payment.service.impl.PaymentCommonService;
import com.myce.payment.service.mapper.PaymentMapper;
import com.myce.payment.service.verification.PaymentVerificationService;
import com.myce.reservation.entity.Reservation;
import com.myce.reservation.entity.code.UserType;
import com.myce.reservation.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentVerificationServiceImpl implements PaymentVerificationService {

    private final PaymentInternalService paymentInternalService;
    private final ReservationRepository reservationRepository;
    private final AdPaymentInfoRepository adPaymentInfoRepository;
    private final ExpoPaymentInfoRepository expoPaymentInfoRepository;
    private final ReservationPaymentInfoRepository reservationPaymentInfoRepository;
    private final PaymentMapper paymentMapper;
    private final MemberExpoService memberExpoService;
    private final MemberAdService memberAdService;
    private final PaymentCommonService paymentCommonService;

    // 카드 결제 검증 및 저장
    @Override
    @Transactional
    public PaymentVerifyResponse verifyPayment(PaymentVerifyInfo verifyInfo) {
        validateTarget(verifyInfo);
        // 1) 결제 사용자 식별 (예약/광고/박람회 구분)
        UserIdentifier userIdentifier = identifyUser(verifyInfo.getTargetType(), verifyInfo.getTargetId());
        if (userIdentifier == null) {
            throw new CustomException(CustomErrorCode.INVALID_PAYMENT_TARGET_TYPE);
        }
        // 2) payment internal로 "검증 + Payment 저장" 요청
        //    - PortOne 호출은 이제 payment 서버가 처리함
        PaymentInternalRequest internalRequest = PaymentInternalRequest.builder()
                .impUid(verifyInfo.getImpUid())
                .merchantUid(verifyInfo.getMerchantUid())
                .amount(verifyInfo.getAmount())
                .targetType(verifyInfo.getTargetType())  // AD / EXPO / RESERVATION
                .targetId(verifyInfo.getTargetId())
                // 예약일 때만 reservationId 유지
                .reservationId(verifyInfo.getTargetType() == PaymentTargetType.RESERVATION
                        ? verifyInfo.getTargetId() : null)
                .build();
        PaymentInternalResponse internalResponse =
                paymentInternalService.verifyAndSave(internalRequest);

        // 3) core는 PaymentInfo만 저장/상태 변경
        PaymentInfoDetailDto paymentInfo =
                savePaymentInfoDetails(verifyInfo, PaymentStatus.SUCCESS, userIdentifier);

        // 4) 응답 반환 (Payment 엔티티는 이제 core에 저장하지 않음)
        return PaymentVerifyResponse.builder()
                .impUid(internalResponse.getImpUid())
                .merchantUid(internalResponse.getMerchantUid())
                .status(paymentInfo.getStatus())
                .amount(paymentInfo.getAmount())
                .reservationId(paymentInfo.getReservationId()) // 예약일 때만 값 있음
                .build();
    }

    // 가상계좌 발급 및 상태 저장 PENDING
    @Override
    @Transactional
    public PaymentVerifyResponse verifyVbankPayment(PaymentVerifyInfo verifyInfo) {
        validateTarget(verifyInfo);
        // 1) 결제 사용자 식별
        UserIdentifier userIdentifier = identifyUser(verifyInfo.getTargetType(), verifyInfo.getTargetId());
        if (userIdentifier == null) {
            throw new CustomException(CustomErrorCode.INVALID_PAYMENT_TARGET_TYPE);
        }

        // 2) payment internal로 "vbank 검증 + Payment 저장" 요청
        PaymentInternalRequest internalRequest = PaymentInternalRequest.builder()
                .impUid(verifyInfo.getImpUid())
                .merchantUid(verifyInfo.getMerchantUid())
                .amount(verifyInfo.getAmount())
                .targetType(verifyInfo.getTargetType())
                .targetId(verifyInfo.getTargetId())
                .reservationId(verifyInfo.getTargetType() == PaymentTargetType.RESERVATION
                        ? verifyInfo.getTargetId() : null)
                .build();

        PaymentInternalResponse internalResponse =
                paymentInternalService.verifyAndSaveVbank(internalRequest);

        // 3) core는 PaymentInfo를 PENDING으로 저장
        PaymentInfoDetailDto paymentInfo =
                savePaymentInfoDetails(verifyInfo, PaymentStatus.PENDING, userIdentifier);

        // 4) 응답 반환
        return PaymentVerifyResponse.builder()
                .impUid(internalResponse.getImpUid())
                .merchantUid(internalResponse.getMerchantUid())
                .status(paymentInfo.getStatus())
                .amount(paymentInfo.getAmount())
                .reservationId(paymentInfo.getReservationId())
                .build();
    }


    // 결제 사용자 식별
    private UserIdentifier identifyUser(PaymentTargetType targetType, Long targetId) {
        return switch (targetType) {
            case RESERVATION -> getUserIdentifierForReservation(targetId);
            case AD, EXPO -> getUserIdentifierToAuthentication();
        };
    }

    private UserIdentifier getUserIdentifierForReservation(long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new CustomException(CustomErrorCode.RESERVATION_NOT_FOUND));
        return UserIdentifier.builder().userType(reservation.getUserType()).userId(reservation.getUserId()).build();
    }

    private UserIdentifier getUserIdentifierToAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new CustomException(CustomErrorCode.REFRESH_TOKEN_NOT_EXIST);
        }
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        return UserIdentifier.builder().userType(UserType.MEMBER).userId(userDetails.getUserId()).build();
    }

    private void validateTarget(PaymentVerifyInfo verifyInfo) {
        if (verifyInfo == null || verifyInfo.getTargetType() == null || verifyInfo.getTargetId() == null) {
            throw new CustomException(CustomErrorCode.INVALID_PAYMENT_TARGET_TYPE);
        }
        if (verifyInfo.getTargetId() <= 0) {
            throw new CustomException(CustomErrorCode.INVALID_PAYMENT_TARGET_TYPE);
        }
    }

    // 결제 대상별 세부 결제 정보 저장(Reservation, Ad, Expo)
    private PaymentInfoDetailDto savePaymentInfoDetails(
            PaymentVerifyInfo verifyInfo, PaymentStatus paymentStatus, UserIdentifier userIdentifier) {
        Long memberId = userIdentifier.getUserId();
        return switch (verifyInfo.getTargetType()) {
            case RESERVATION -> {
                ReservationPaymentInfo paymentInfo = saveReservationPaymentInfo(verifyInfo, paymentStatus);
                yield new PaymentInfoDetailDto(
                        paymentInfo.getStatus().name(), paymentInfo.getTotalAmount(),
                        paymentInfo.getReservation().getId());
            }
            case AD -> {
                AdPaymentInfo paymentInfo = saveAdPaymentInfo(verifyInfo, paymentStatus, memberId);
                yield new PaymentInfoDetailDto(paymentInfo.getStatus().name(), paymentInfo.getTotalAmount());
            }
            case EXPO -> {
                ExpoPaymentInfo paymentInfo = saveExpoPaymentInfo(verifyInfo, paymentStatus, memberId);
                yield new PaymentInfoDetailDto(paymentInfo.getStatus().name(), paymentInfo.getTotalAmount());
            }
        };
    }

    private ReservationPaymentInfo saveReservationPaymentInfo(
            PaymentVerifyInfo verifyInfo, PaymentStatus paymentStatus) {


        // 예약 정보 조회
        Reservation reservation = reservationRepository.findById(verifyInfo.getTargetId())
                .orElseThrow(() -> new CustomException(CustomErrorCode.RESERVATION_NOT_FOUND));


        // 비회원 적림금 지급 방지 추가
        if (reservation.getUserType().equals(UserType.GUEST)) verifyInfo.setSavedMileage(0);

        int paidAmount = verifyInfo.getAmount();
                // PaymentInfo 생성 후 저장
        ReservationPaymentInfo reservationPaymentInfo = paymentMapper.toReservationPaymentInfo(
                reservation, paidAmount, paymentStatus, verifyInfo.getUsedMileage(), verifyInfo.getSavedMileage());
        reservationPaymentInfo = reservationPaymentInfoRepository.save(reservationPaymentInfo);

        // 결제 완료 시 알림 발송 (회원만)
        if (paymentStatus.equals(PaymentStatus.SUCCESS) && reservation.getUserType().equals(UserType.MEMBER)) {
            paymentCommonService.sendAlert(reservation, paidAmount);
        }

        return reservationPaymentInfo;
    }

    private AdPaymentInfo saveAdPaymentInfo(
            PaymentVerifyInfo verifyInfo, PaymentStatus paymentStatus, Long memberId) {
        // 이미 PaymentInfo 있으므로 SUCCESS로만
        AdPaymentInfo adPaymentInfo = adPaymentInfoRepository.findByAdvertisementId(verifyInfo.getTargetId())
                .orElseThrow(() -> new CustomException(CustomErrorCode.PAYMENT_INFO_NOT_FOUND));
        adPaymentInfo.updateStatus(paymentStatus);
        adPaymentInfo = adPaymentInfoRepository.save(adPaymentInfo);

        // completeAdvertisementPayment 호출
        memberAdService.completeAdvertisementPayment(memberId, verifyInfo.getTargetId());
        return adPaymentInfo;
    }

    private ExpoPaymentInfo saveExpoPaymentInfo(
            PaymentVerifyInfo verifyInfo, PaymentStatus paymentStatus, Long memberId) {
        // 이미 PaymentInfo 있으므로 SUCCESS로만
        ExpoPaymentInfo expoPaymentInfo = expoPaymentInfoRepository.findByExpoId(verifyInfo.getTargetId())
                .orElseThrow(() -> new CustomException(CustomErrorCode.PAYMENT_INFO_NOT_FOUND));
        expoPaymentInfo.updateStatus(paymentStatus);
        expoPaymentInfo = expoPaymentInfoRepository.save(expoPaymentInfo);


        // completeExpoPayment 호출
        memberExpoService.completeExpoPayment(memberId, verifyInfo.getTargetId());

        return expoPaymentInfo;
    }
}
