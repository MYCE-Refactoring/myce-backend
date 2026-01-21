package com.myce.refund.service.impl;

import com.myce.client.payment.service.RefundInternalService;
import com.myce.common.exception.CustomException;
import com.myce.common.exception.CustomErrorCode;
import com.myce.expo.entity.Expo;
import com.myce.expo.entity.Ticket;
import com.myce.expo.entity.type.ExpoStatus;
import com.myce.expo.repository.ExpoRepository;
import com.myce.expo.repository.TicketRepository;
import com.myce.member.dto.MileageUpdateRequest;
import com.myce.member.service.MemberMileageService;
import com.myce.notification.service.NotificationService;
import com.myce.payment.dto.PaymentRefundRequest;
import com.myce.payment.dto.RefundInternalRequest;
import com.myce.payment.entity.ReservationPaymentInfo;
import com.myce.payment.entity.type.PaymentTargetType;
import com.myce.payment.repository.ReservationPaymentInfoRepository;
import com.myce.payment.service.refund.PaymentRefundService;
import com.myce.reservation.entity.Reservation;
import com.myce.reservation.entity.code.ReservationStatus;
import com.myce.reservation.repository.ReservationRepository;
import com.myce.refund.dto.RefundRequestDto;
import com.myce.refund.dto.ReservationRefundCalculation;
import com.myce.refund.service.RefundRequestService;
import com.myce.refund.service.ReservationRefundCalculationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class RefundRequestServiceImpl implements RefundRequestService {

    // refund 원장/PortOne 호출은 payment internal이 담당, core는 상태/알림/후처리만 담당
    private final RefundInternalService refundInternalService;
    // 즉시 환불(예약 환불)은 payment internal 호출로 위임
    private final PaymentRefundService paymentRefundService;
    private final ExpoRepository expoRepository;
    private final ReservationRepository reservationRepository;
    private final ReservationRefundCalculationService refundCalculationService;
    private final MemberMileageService memberMileageService;
    private final ReservationPaymentInfoRepository reservationPaymentInfoRepository;
    private final TicketRepository ticketRepository;
    private final NotificationService notificationService;

/*
  - 기존: PaymentRepository/RefundRepository 직접 조회 + Refund 저장
  - 변경: RefundInternalService로 요청 → payment에서 PENDING 생성/검증
  - core는 상태 변경/알림/마일리지/티켓만 처리
 */

    //  refundRepository로 PENDING 저장하지 않고, payment internal에 PENDING 생성 맡김.
    @Override
    public void createRefundRequest(Long memberId, Long expoId, RefundRequestDto requestDto) {
        // 1) 엑스포 조회
        Expo expo = expoRepository.findById(expoId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 엑스포입니다."));

        // 2) 환불 가능 상태 확인
        validateRefundEligibility(expo.getStatus(), expo);

        // 3) 환불 타입 결정 (부분/전체)
        boolean isPartialRefund = determineRefundType(expo.getStatus());
        System.out.println("[환불 신청] 엑스포 ID: " + expoId + ", 현재 상태: " + expo.getStatus()
                + ", 부분환불 여부: " + isPartialRefund);

        // 4) 알림 (core 책임)
        try {
            String oldStatus = expo.getStatus().name();
            notificationService.sendExpoStatusChangeNotification(
                    expoId, expo.getTitle(), oldStatus, "PENDING_CANCEL");
        } catch (Exception e) {
            log.warn("박람회 환불 신청 알림 전송 실패 - expoId: {}, 오류: {}", expoId, e.getMessage());
        }

        // 5) 엑스포 상태 변경
        expo.updateStatus(ExpoStatus.PENDING_CANCEL);

        // 6) payment internal로 impUid 조회 (payment DB 직접 접근 제거)
        String impUid = refundInternalService.getImpUid(PaymentTargetType.EXPO, expoId);

        // 7) internal 환불 신청(PENDING 생성) 요청
        RefundInternalRequest internalRequest = RefundInternalRequest.builder()
                .impUid(impUid)
                .cancelAmount(requestDto.getAmount())
                .reason(requestDto.getReason())
                .build();

        refundInternalService.requestRefund(internalRequest);
    }

    /*
        createReservationRefund() (예약 즉시 환불)
        바꾸는 이유:
        PaymentRepository/RefundRepository 직접 접근 제거.
        impUid는 internal에서 조회하고, 실제 환불은 internal로 위임.
     */

    @Override
    public void createReservationRefund(Long memberId, Long reservationId, String reason) {
        // 1) 예약 조회/검증
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new CustomException(CustomErrorCode.RESERVATION_NOT_FOUND));

        ReservationPaymentInfo reservationInfo = reservationPaymentInfoRepository.findByReservationId(reservationId)
                .orElseThrow(() -> new CustomException(CustomErrorCode.RESERVATION_NOT_FOUND));

        // 2) 예약 소유자 확인
        if (!reservation.getUserId().equals(memberId)) {
            throw new CustomException(CustomErrorCode.RESERVATION_NOT_FOUND);
        }

        // 3) 예약 상태 확인
        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new CustomException(CustomErrorCode.RESERVATION_STATUS_INVALID);
        }

        // 4) 환불 금액 계산
        ReservationRefundCalculation calculation = refundCalculationService.calculateRefundAmount(reservationId);
        if (calculation.getActualRefundAmount() <= 0) {
            throw new CustomException(CustomErrorCode.REFUND_NOT_ALLOWED);
        }

        // 5) impUid 조회 (payment internal)
        String impUid = refundInternalService.getImpUid(PaymentTargetType.RESERVATION, reservationId);

        // 6) 환불 요청 생성
        PaymentRefundRequest refundRequest = PaymentRefundRequest.builder()
                .impUid(impUid)
                .cancelAmount(calculation.getActualRefundAmount())
                .reason(reason)
                .build();

        // 7) payment internal로 환불 실행
        paymentRefundService.refundPayment(refundRequest);

        // 8) 마일리지/티켓/예약 상태 처리 (core 책임)
        MileageUpdateRequest mileageRequest = new MileageUpdateRequest(
                reservationInfo.getUsedMileage(),
                reservationInfo.getSavedMileage()
        );
        memberMileageService.revertMileageForReservationRefund(memberId, mileageRequest);

        Ticket ticket = reservation.getTicket();
        ticket.restoreQuantity(reservation.getQuantity());
        ticketRepository.save(ticket);

        reservation.updateStatus(ReservationStatus.CANCELLED);
        reservationRepository.save(reservation);
    }

    /**
     * 환불 가능 상태인지 확인
     * PUBLISHED 상태의 경우 7일 규칙도 함께 검증
     * 
     * @param expoStatus 현재 엑스포 상태
     * @param expo 엑스포 엔티티 (7일 규칙 확인용)
     * @throws IllegalStateException 환불 불가능한 상태인 경우
     */
    private void validateRefundEligibility(ExpoStatus expoStatus, Expo expo) {
        switch (expoStatus) {
            case PUBLISHED:
                // 게시중일 때만 7일 규칙 적용
                validateSevenDayRule(expo);
                break;
            case PENDING_PUBLISH:
                // 게시 대기는 7일 규칙 적용 안함 (언제든 취소 가능)
                break;
            case PUBLISH_ENDED:
            case COMPLETED:
            case CANCELLED:
            case REJECTED:
                throw new IllegalStateException("현재 상태에서는 환불 신청이 불가능합니다.");
            case PENDING_CANCEL:
                throw new IllegalStateException("이미 환불 신청이 진행 중입니다.");
            default:
                // 기타 환불 가능한 상태
                break;
        }
    }
    
    /**
     * 7일 규칙 검증 (게시중 박람회만 적용)
     * 개최일 7일 전부터는 환불 불가
     * 
     * @param expo 엑스포 엔티티
     * @throws CustomException 7일 이내인 경우
     */
    private void validateSevenDayRule(Expo expo) {
        LocalDate today = LocalDate.now();
        long daysUntilStart = ChronoUnit.DAYS.between(today, expo.getStartDate());
        
        if (daysUntilStart < 7) {
            throw new CustomException(CustomErrorCode.REFUND_SEVEN_DAY_RULE_VIOLATION);
        }
    }
    
    /**
     * 엑스포 상태에 따른 환불 타입 결정
     * @param expoStatus 현재 엑스포 상태
     * @return true: 부분 환불, false: 전액 환불
     */
    private boolean determineRefundType(ExpoStatus expoStatus) {
        switch (expoStatus) {
            case PENDING_PUBLISH:
                // 게시 대기 중: 전액 환불 (등록금 + 이용료)
                return false;
            case PUBLISHED:
                // 게시 중: 부분 환불 (사용한 일수 제외한 이용료만)
                return true;
            default:
                // 기타 상태는 부분 환불로 처리
                return true;
        }
    }
}
