package com.myce.expo.service.platform.impl;

import com.myce.client.payment.service.RefundInternalService;
import com.myce.common.entity.BusinessProfile;
import com.myce.common.entity.RejectInfo;
import com.myce.common.entity.type.TargetType;
import com.myce.common.exception.CustomErrorCode;
import com.myce.common.exception.CustomException;
import com.myce.common.repository.BusinessProfileRepository;
import com.myce.common.repository.RejectInfoRepository;
import com.myce.payment.dto.RefundInternalResponse;
import com.myce.expo.entity.Expo;
import com.myce.expo.entity.type.ExpoStatus;
import com.myce.expo.repository.ExpoRepository;
import com.myce.expo.service.info.ExpoStatusService;
import com.myce.expo.service.platform.PlatformExpoManageService;
import com.myce.expo.service.platform.mapper.ExpoPaymentInfoMapper;
import com.myce.expo.service.platform.mapper.RejectInfoMapper;
import com.myce.client.notification.service.NotificationService;
import com.myce.payment.entity.ExpoPaymentInfo;
import com.myce.payment.entity.type.PaymentStatus;
import com.myce.payment.entity.type.PaymentTargetType;
import com.myce.payment.entity.type.RefundStatus;
import com.myce.payment.repository.ExpoPaymentInfoRepository;
import com.myce.payment.service.refund.PaymentRefundService;
import com.myce.payment.dto.PaymentRefundRequest;
import com.myce.expo.dto.ExpoPaymentPreviewResponse;
import com.myce.expo.service.platform.mapper.ExpoPaymentPreviewMapper;
import com.myce.settlement.service.SettlementPlatformAdminService;
import com.myce.system.entity.ExpoFeeSetting;
import com.myce.system.repository.ExpoFeeSettingRepository;
import com.myce.reservation.entity.Reservation;
import com.myce.reservation.entity.code.ReservationStatus;
import com.myce.reservation.repository.ReservationRepository;
import com.myce.payment.entity.ReservationPaymentInfo;
import com.myce.payment.repository.ReservationPaymentInfoRepository;
import com.myce.expo.entity.Ticket;
import com.myce.expo.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 플랫폼 관리자용 박람회 신청 관리 서비스 구현체
 * 박람회 승인/거절 처리를 담당
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlatformExpoManageServiceImpl implements PlatformExpoManageService {

    // 상수 정의
    private static final String CANCELLATION_REFUND_REASON = "박람회 취소 승인으로 인한 전액 환불";
    private static final String INDIVIDUAL_RESERVATION_REFUND_REASON = "박람회 취소로 인한 개인 예약 환불";
    
    // 의존성 주입
    private final ExpoRepository expoRepository;
    private final ExpoPaymentInfoRepository expoPaymentInfoRepository;
    private final RejectInfoRepository rejectInfoRepository;
    private final ExpoFeeSettingRepository expoFeeSettingRepository;
    private final SettlementPlatformAdminService settlementPlatformAdminService;
    private final BusinessProfileRepository businessProfileRepository;
    
    // 취소/환불 관련 의존성
    private final RefundInternalService refundInternalService;
    private final PaymentRefundService paymentRefundService;
    
    // 개인 예약자 환불 관련 의존성
    private final ReservationRepository reservationRepository;
    private final ReservationPaymentInfoRepository reservationPaymentInfoRepository;
    private final TicketRepository ticketRepository;
    private final ExpoStatusService expoStatusService;

    private final NotificationService notificationService;
    /**
     * 박람회 신청 승인 처리
     * - 박람회 상태를 PENDING_APPROVAL -> PENDING_PAYMENT로 변경
     * - ExpoPaymentInfo 생성하여 결제 대기 상태로 설정
     * 
     * @param expoId 승인할 박람회 ID
     */
    @Override
    @Transactional
    public void approveExpoApplication(Long expoId) {
        // 1. 요청 정보 로깅


        // 2. 박람회 엔티티 조회 및 검증
        Expo expo = expoRepository.findById(expoId)
                .orElseThrow(() -> {
                    log.error("존재하지 않는 박람회 ID로 승인 시도: {}", expoId);
                    return new CustomException(CustomErrorCode.EXPO_NOT_FOUND);
                });
        
        // 박람회 상태 검증
        expoStatusService.verifyApproveExpo(expo);

        // 3. 활성화된 수수료 설정 조회
        ExpoFeeSetting feeSetting = expoFeeSettingRepository.findByIsActiveTrue()
                .orElseThrow(() -> {
                    log.error("활성화된 박람회 수수료 설정이 존재하지 않음");
                    return new CustomException(CustomErrorCode.FEE_SETTING_NOT_FOUND);
                });
        
        // 4. 박람회 일수 및 총 금액 계산
        int totalDays = calculateTotalDays(expo);
        Integer totalAmount = calculateTotalAmount(expo, feeSetting, totalDays);
        
        
        // 5. ExpoPaymentInfo 생성 (Mapper 사용)
        ExpoPaymentInfo paymentInfo = ExpoPaymentInfoMapper.toEntity(expo, feeSetting, totalDays, totalAmount);
        
        expoPaymentInfoRepository.save(paymentInfo);
        
        // 6. 박람회 상태 변경 (Entity 메서드 사용)
        ExpoStatus oldStatus = expo.getStatus();
        expo.approve();
        ExpoStatus newStatus = expo.getStatus();

        notificationService.notifyExpoStatusChange(expo, oldStatus, newStatus);
        
        log.info("박람회 신청 승인 완료 - expoId: {}, totalAmount: {}", expoId, totalAmount);
    }

    /**
     * 박람회 신청 거절 처리
     * - 박람회 상태를 PENDING_APPROVAL -> REJECTED로 변경
     * - RejectInfo 생성하여 거절 사유 저장
     * 
     * @param expoId 거절할 박람회 ID
     * @param reason 거절 사유
     */
    @Override
    public void rejectExpoApplication(Long expoId, String reason) {
        // 1. 요청 정보 로깅
        
        // 2. 박람회 엔티티 조회 및 검증
        Expo expo = expoRepository.findById(expoId)
                .orElseThrow(() -> {
                    log.error("존재하지 않는 박람회 ID로 거절 시도: {}", expoId);
                    return new CustomException(CustomErrorCode.EXPO_NOT_FOUND);
                });
        // 박람회 상태 검증
        expoStatusService.verifyRejectExpo(expo);
        
        // 3. 거절 정보 생성 (Mapper 사용)
        RejectInfo rejectInfo = RejectInfoMapper.toEntity(expo.getId(), reason);
        
        rejectInfoRepository.save(rejectInfo);
        
        // 4. 박람회 상태 변경 (Entity 메서드 사용)
        ExpoStatus oldStatus = expo.getStatus();
        expo.reject();
        ExpoStatus newStatus = expo.getStatus();
        notificationService.notifyExpoStatusChange(expo, oldStatus, newStatus);

        
        log.info("박람회 신청 거절 완료 - expoId: {}", expoId);
    }

    /**
     * 박람회 취소 승인 처리
     * - PENDING_CANCEL 상태의 박람회를 CANCELLED로 변경
     * - 원래 상태(PUBLISHED/PENDING_PUBLISH)에 따라 다른 환불 처리
     * - PENDING_PUBLISH: 박람회 주최자만 전액 환불
     * - PUBLISHED: 개별 예약자 환불 + 박람회 주최자 부분 환불
     * 
     * @param expoId 취소 승인할 박람회 ID
     */
    @Override
    @Transactional
    public void approveCancellation(Long expoId) {
        // 1. 요청 정보 로깅
        log.info("박람회 취소 승인 요청 - expoId: {}", expoId);
        
        // 2. 박람회 조회 및 상태 검증
        Expo expo = validateExpoForCancellation(expoId);

        // 박람회 상태 검증
        expoStatusService.verifyApproveCancellation(expo);

        // 3. 결제 정보 조회
        ExpoPaymentInfo paymentInfo = getExpoPaymentInfo(expoId);
        
        // 4. 환불 신청 정보로 원래 상태 확인
        boolean wasPublishedStatus = determineOriginalExpoStatus(expoId);
        
        if (wasPublishedStatus) {
            // 5-1. PUBLISHED 상태였던 경우: 개별 예약자 환불 + 부분 환불
            log.info("PUBLISHED 상태 박람회 취소 승인 처리 - expoId: {}", expoId);
            
            // 개별 예약자들 환불 처리 (선처리)
            processIndividualReservationRefunds(expoId);
            
            // 박람회 주최자 부분 환불 처리
            processRefundViaPaymentInternal(expoId, paymentInfo);

            // 박람회 상태 변경
            ExpoStatus oldStatus = expo.getStatus();
            expo.approveCancellation();
            ExpoStatus newStatus = expo.getStatus();
            notificationService.notifyExpoStatusChange(expo, oldStatus, newStatus);

            // 결제 정보 상태를 PARTIAL_REFUNDED로 변경 (개별 예약자 환불이 있었으므로)
            paymentInfo.setStatus(PaymentStatus.PARTIAL_REFUNDED);
            
        } else {
            // 5-2. PENDING_PUBLISH 상태였던 경우: 기존 로직 그대로 (전액 환불)
            log.info("PENDING_PUBLISH 상태 박람회 취소 승인 처리 - expoId: {}", expoId);
            
            // payment internal 환불 처리
            processRefundViaPaymentInternal(expoId, paymentInfo);
            
            // 박람회 상태 변경 (PENDING_CANCEL -> CANCELLED)
            ExpoStatus oldStatus = expo.getStatus();
            expo.approveCancellation();
            ExpoStatus newStatus = expo.getStatus();
            notificationService.notifyExpoStatusChange(expo, oldStatus, newStatus);
            
            // 결제 정보 상태 변경 (SUCCESS -> REFUNDED)
            paymentInfo.setStatus(PaymentStatus.REFUNDED);
        }
        
        log.info("박람회 취소 승인 완료 - expoId: {}, 원래상태: {}", 
                expoId, wasPublishedStatus ? "PUBLISHED" : "PENDING_PUBLISH");
    }

    /**
     * 박람회 취소 승인 가능 여부 검증
     * 
     * @param expoId 박람회 ID
     * @return 검증된 박람회 엔티티
     */
    private Expo validateExpoForCancellation(Long expoId) {
        Expo expo = expoRepository.findById(expoId)
                .orElseThrow(() -> {
                    log.error("박람회 취소 승인 실패 - 존재하지 않는 박람회 ID: {}", expoId);
                    return new CustomException(CustomErrorCode.EXPO_NOT_FOUND);
                });
        return expo;
    }
    
    /**
     * 박람회 결제 정보 조회
     * 
     * @param expoId 박람회 ID
     * @return 박람회 결제 정보
     */
    private ExpoPaymentInfo getExpoPaymentInfo(Long expoId) {
        return expoPaymentInfoRepository.findByExpoId(expoId)
                .orElseThrow(() -> {
                    log.error("박람회 취소 승인 실패 - 결제 정보 없음: expoId {}", expoId);
                    return new CustomException(CustomErrorCode.PAYMENT_INFO_NOT_FOUND);
                });
    }
    
    /**
     * payment internal 환불 처리 및 Refund 테이블 갱신
     * 
     * @param expoId 박람회 ID
     * @param paymentInfo 박람회 결제 정보
     */
    private void processRefundViaPaymentInternal(Long expoId, ExpoPaymentInfo paymentInfo) {
        // 1) 환불 신청 존재 확인 (internal 조회)
        RefundInternalResponse refundInfo =
                refundInternalService.getRefundByTarget(PaymentTargetType.EXPO, expoId);

        // ※ 필요하면 상태 체크 (PENDING만 승인 가능)
        if (refundInfo.getStatus() != RefundStatus.PENDING) {
            throw new CustomException(CustomErrorCode.ALREADY_REFUNDED);
        }

        // 2) impUid 조회 (payment DB 직접 접근 제거)
        String impUid = refundInternalService.getImpUid(PaymentTargetType.EXPO, expoId);
        
        // 3. 환불 요청 생성
        PaymentRefundRequest refundRequest = PaymentRefundRequest.builder()
                .impUid(impUid)
                .cancelAmount(null)
                .reason(CANCELLATION_REFUND_REASON)
                .build();

        // 4) payment internal로 환불 실행
        paymentRefundService.refundPayment(refundRequest);
    }

    /**
     * 박람회 정산 승인 처리
     * - Settlement 로직을 SettlementPlatformAdminService로 완전 위임
     * 
     * @param expoId 정산 승인할 박람회 ID
     * @param adminMemberId 현재 로그인한 플랫폼 관리자 ID
     */
    @Override
    @Transactional
    public void approveSettlement(Long expoId, Long adminMemberId) {
        log.info("박람회 정산 승인 요청 - expoId: {}, adminMemberId: {}", expoId, adminMemberId);
        
        // Settlement 로직은 SettlementPlatformAdminService로 완전 위임
        settlementPlatformAdminService.approveSettlement(expoId, adminMemberId);
        
        log.info("박람회 정산 승인 위임 완료 - expoId: {}, adminMemberId: {}", expoId, adminMemberId);
    }

    /**
     * 박람회 총 일수 계산
     * 게시 기간(displayStartDate ~ displayEndDate) 기준으로 계산
     * 
     * @param expo 박람회 엔티티
     * @return 총 일수
     */
    private int calculateTotalDays(Expo expo) {
        long days = ChronoUnit.DAYS.between(expo.getDisplayStartDate(), expo.getDisplayEndDate()) + 1;
        return (int) days;
    }

    /**
     * 박람회 총 금액 계산
     * 보증금 + (일일사용료 × 일수) + 프리미엄보증금
     * 
     * @param expo 박람회 엔티티
     * @param feeSetting 수수료 설정
     * @param totalDays 총 일수
     * @return 총 금액
     */
    private Integer calculateTotalAmount(Expo expo, ExpoFeeSetting feeSetting, int totalDays) {
        Integer deposit = feeSetting.getDeposit() != null ? feeSetting.getDeposit() : 0;
        Integer dailyUsageFee = feeSetting.getDailyUsageFee() != null ? feeSetting.getDailyUsageFee() : 0;
        Integer dailyFee = dailyUsageFee * totalDays;
        
        if (expo.getIsPremium()) {
            // 프리미엄일 경우: 기본 등록금 + 프리미엄 이용료 + 사용료
            Integer premiumDeposit = feeSetting.getPremiumDeposit() != null ? feeSetting.getPremiumDeposit() : 0;
            return deposit + premiumDeposit + dailyFee;
        } else {
            // 기본일 경우: 기본 등록금 + 사용료
            return deposit + dailyFee;
        }
    }
    
    /**
     * 박람회 승인 시 결제 정보 미리보기
     * DB 저장 없이 계산된 결제 정보를 반환
     * 팀 코드 스타일: 체계적인 로깅과 예외 처리
     * 
     * @param expoId 박람회 ID
     * @return 결제 정보 미리보기 응답
     */
    @Override
    @Transactional(readOnly = true)
    public ExpoPaymentPreviewResponse getPaymentPreview(Long expoId) {
        // 1. 요청 정보 로깅
        log.debug("결제 정보 미리보기 요청 - expoId: {}", expoId);
        
        // 2. 박람회 엔티티 조회 및 검증
        Expo expo = expoRepository.findById(expoId)
                .orElseThrow(() -> {
                    log.error("존재하지 않는 박람회 ID로 결제 정보 미리보기 시도: {}", expoId);
                    return new CustomException(CustomErrorCode.EXPO_NOT_FOUND);
                });
        
        // 3. 상태 검증 (PENDING_APPROVAL 상태에서만 미리보기 가능)
        if (expo.getStatus() != ExpoStatus.PENDING_APPROVAL) {
            log.warn("잘못된 상태에서 결제 정보 미리보기 시도 - expoId: {}, status: {}", expoId, expo.getStatus());
            throw new CustomException(CustomErrorCode.INVALID_EXPO_STATUS);
        }
        
        // 4. 활성화된 수수료 설정 조회
        ExpoFeeSetting feeSetting = expoFeeSettingRepository.findByIsActiveTrue()
                .orElseThrow(() -> {
                    log.error("활성화된 박람회 수수료 설정이 존재하지 않음");
                    return new CustomException(CustomErrorCode.FEE_SETTING_NOT_FOUND);
                });
        
        // 5. 사업자 정보 조회 (optional)
        BusinessProfile businessProfile = businessProfileRepository
                .findByTargetIdAndTargetType(expoId, TargetType.EXPO)
                .orElse(null);
        
        // 6. 기존 private 메서드 활용하여 계산
        int totalDays = calculateTotalDays(expo);
        Integer totalAmount = calculateTotalAmount(expo, feeSetting, totalDays);
        
        log.debug("결제 정보 계산 완료 - expoId: {}, totalDays: {}, totalAmount: {}", 
                expoId, totalDays, totalAmount);
        
        // 7. Mapper를 통해 DTO 변환
        return ExpoPaymentPreviewMapper.toDto(expo, businessProfile, feeSetting, totalDays, totalAmount);
    }
    
    /**
     * 게시중 박람회 취소 시 개인 예약자들 환불 처리
     * PUBLISHED 상태 박람회 취소 승인 시 모든 예약자에게 개별 환불 처리
     * 
     * @param expoId 취소 승인할 박람회 ID
     */
    private void processIndividualReservationRefunds(Long expoId) {
        log.info("개인 예약자 환불 처리 시작 - expoId: {}", expoId);
        
        // 1. 해당 박람회의 모든 예약 조회 후 CONFIRMED 상태만 필터링
        List<Reservation> allReservations = reservationRepository.findByExpoId(expoId);
        List<Reservation> confirmedReservations = allReservations.stream()
                .filter(reservation -> reservation.getStatus() == ReservationStatus.CONFIRMED)
                .toList();
        
        if (confirmedReservations.isEmpty()) {
            log.info("환불 처리할 예약이 없음 - expoId: {}", expoId);
            return;
        }
        
        log.info("환불 처리 대상 예약 수: {} - expoId: {}", confirmedReservations.size(), expoId);
        
        // 2. 각 예약별 환불 처리
        for (Reservation reservation : confirmedReservations) {
            try {
                refundIndividualReservation(reservation);
                log.debug("개별 예약 환불 완료 - reservationId: {}, expoId: {}", 
                        reservation.getId(), expoId);
            } catch (Exception e) {
                log.error("개별 예약 환불 실패 - reservationId: {}, expoId: {}, error: {}", 
                        reservation.getId(), expoId, e.getMessage());
                throw new CustomException(CustomErrorCode.PORTONE_REFUND_FAILED);
            }
        }
        
        log.info("개인 예약자 환불 처리 완료 - expoId: {}, 처리된 예약 수: {}", 
                expoId, confirmedReservations.size());
    }
    
    /**
     * 개별 예약자 환불 처리
     * 예약 상태 변경, 결제 정보 환불 처리, payment internal 환불 실행, 티켓 재고 복구
     * 
     * @param reservation 환불 처리할 예약 정보
     */
    private void refundIndividualReservation(Reservation reservation) {
        Long reservationId = reservation.getId();
        reservation.updateStatus(ReservationStatus.CANCELLED);
        log.debug("예약 상태 변경 완료 - reservationId: {}, status: CANCELLED", reservationId);
        
        // 2. 예약 결제 정보 조회 및 상태 변경
        ReservationPaymentInfo paymentInfo = reservationPaymentInfoRepository
                .findByReservationId(reservationId)
                .orElseThrow(() -> new CustomException(CustomErrorCode.PAYMENT_INFO_NOT_FOUND));;
        
        // 3. 결제 정보 상태를 REFUNDED로 변경
        paymentInfo.setStatus(PaymentStatus.REFUNDED);
        log.debug("예약 결제 정보 상태 변경 완료 - reservationId: {}, status: REFUNDED", reservationId);

        // 3) impUid 조회 (payment internal)
        String impUid = refundInternalService.getImpUid(PaymentTargetType.RESERVATION, reservationId);
        
        // 6. payment internal 환불 실행
        PaymentRefundRequest refundRequest = PaymentRefundRequest.builder()
                .impUid(impUid)
                .cancelAmount(null) // null = 전액 환불
                .reason(INDIVIDUAL_RESERVATION_REFUND_REASON)
                .build();

        paymentRefundService.refundPayment(refundRequest);

        // 7. 티켓 재고 복구
        restoreTicketInventory(reservation);
        
        log.debug("개별 예약 환불 처리 완료 - reservationId: {}", reservationId);
    }
    
    /**
     * 티켓 재고 복구 처리
     * 예약 취소로 인해 판매된 티켓 수량을 다시 판매 가능 재고로 복구
     * 
     * @param reservation 취소된 예약 정보
     */
    private void restoreTicketInventory(Reservation reservation) {
        Long reservationId = reservation.getId();
        Long ticketId = reservation.getTicket().getId();
        Integer reservedQuantity = reservation.getQuantity();
        
        log.debug("티켓 재고 복구 시작 - reservationId: {}, ticketId: {}, quantity: {}", 
                reservationId, ticketId, reservedQuantity);
        
        // 1. 티켓 엔티티 조회
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> {
                    log.error("티켓 정보 없음 - ticketId: {}", ticketId);
                    return new CustomException(CustomErrorCode.TICKET_NOT_EXIST);
                });
        
        // 2. 재고 복구 실행
        Integer beforeQuantity = ticket.getRemainingQuantity();
        ticket.restoreQuantity(reservedQuantity);
        Integer afterQuantity = ticket.getRemainingQuantity();
        
        log.debug("티켓 재고 복구 완료 - reservationId: {}, ticketId: {}, " +
                "복구 수량: {}, 복구 전: {}, 복구 후: {}", 
                reservationId, ticketId, reservedQuantity, beforeQuantity, afterQuantity);
    }
    
    /**
     * 박람회 원래 상태 확인
     * Refund 테이블의 isPartial 필드로 원래 PUBLISHED였는지 PENDING_PUBLISH였는지 확인
     * 
     * @param expoId 박람회 ID
     * @return true: 원래 PUBLISHED 상태, false: 원래 PENDING_PUBLISH 상태
     */
    private boolean determineOriginalExpoStatus(Long expoId) {
        // 1) internal에서 환불 정보 조회
        RefundInternalResponse refund =
                refundInternalService.getRefundByTarget(PaymentTargetType.EXPO, expoId);

        // 2) isPartial == true면 원래 PUBLISHED, false면 PENDING_PUBLISH
        boolean wasPublished = Boolean.TRUE.equals(refund.getIsPartial());

        log.debug("박람회 원래 상태 확인 - expoId: {}, isPartial: {}, 원래상태: {}",
                expoId, wasPublished, wasPublished ? "PUBLISHED" : "PENDING_PUBLISH");

        return wasPublished;
    }
}
