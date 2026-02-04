package com.myce.expo.service.admin.impl;

import com.myce.client.payment.service.RefundInternalService;
import com.myce.common.entity.BusinessProfile;
import com.myce.common.entity.type.TargetType;
import com.myce.common.exception.CustomErrorCode;
import com.myce.common.exception.CustomException;
import com.myce.common.repository.BusinessProfileRepository;
import com.myce.expo.entity.AdminCode;
import com.myce.expo.entity.AdminPermission;
import com.myce.expo.entity.Expo;
import com.myce.expo.entity.Ticket;
import com.myce.expo.entity.type.ExpoStatus;
import com.myce.expo.repository.AdminCodeRepository;
import com.myce.expo.repository.AdminPermissionRepository;
import com.myce.expo.repository.ExpoRepository;
import com.myce.expo.repository.TicketRepository;
import com.myce.expo.service.admin.ExpoLifeCycleService;
import com.myce.member.dto.expo.*;
import com.myce.member.entity.Member;
import com.myce.member.entity.type.Role;
import com.myce.member.mapper.expo.*;
import com.myce.member.repository.MemberRepository;
import com.myce.client.notification.service.NotificationService;
import com.myce.payment.dto.RefundInternalResponse;
import com.myce.payment.entity.ExpoPaymentInfo;
import com.myce.payment.entity.type.PaymentStatus;
import com.myce.payment.entity.type.PaymentTargetType;
import com.myce.payment.repository.ExpoPaymentInfoRepository;
import com.myce.reservation.entity.Reservation;
import com.myce.reservation.entity.code.ReservationStatus;
import com.myce.reservation.repository.ReservationRepository;
import com.myce.settlement.entity.Settlement;
import com.myce.settlement.repository.SettlementRepository;
import com.myce.settlement.service.SettlementExpoAdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExpoLifeCycleServiceImpl implements ExpoLifeCycleService {

    private final RefundInternalService refundInternalService;
    private final MemberExpoMapper memberExpoMapper;
    private final MemberExpoDetailMapper memberExpoDetailMapper;
    private final ExpoPaymentDetailMapper expoPaymentDetailMapper;
    private final ExpoAdminCodeMapper expoAdminCodeMapper;
    private final ExpoSettlementReceiptMapper expoSettlementReceiptMapper;
    private final BusinessProfileRepository businessProfileRepository;
    private final ExpoPaymentInfoRepository expoPaymentInfoRepository;
    private final ExpoRepository expoRepository;
    private final TicketRepository ticketRepository;
    private final AdminCodeRepository adminCodeRepository;
    private final ExpoRefundReceiptMapper expoRefundReceiptMapper;
    private final SettlementExpoAdminService settlementExpoAdminService;
    private final SettlementRepository settlementRepository;
    private final MemberRepository memberRepository;
    private final AdminPermissionRepository adminPermissionRepository;
    private final ReservationRepository reservationRepository;


    private final NotificationService notificationService;

    @Override
    public Page<MemberExpoResponse> getMemberExpos(Long memberId, Pageable pageable) {
        Page<Expo> expos = expoRepository.findByMemberIdOrderByCreatedAtDesc(memberId, pageable);
        return expos.map(memberExpoMapper::toMemberExpoResponse);
    }

    @Override
    public MemberExpoDetailResponse getMemberExpoDetail(Long memberId, Long expoId) {
        // 박람회가 해당 회원의 것인지 확인
        Expo expo = expoRepository.findById(expoId)
                .orElseThrow(() -> new CustomException(CustomErrorCode.EXPO_NOT_FOUND));

        if (!expo.getMember().getId().equals(memberId)) {
            throw new CustomException(CustomErrorCode.EXPO_ACCESS_DENIED);
        }

        // 결제 정보 조회
        ExpoPaymentInfo paymentInfo = expoPaymentInfoRepository.findByExpoId(expoId)
                .orElse(null);

        // 티켓 목록 조회
        List<Ticket> tickets = ticketRepository.findByExpoIdOrderByCreatedAtAsc(expoId);

        // 사업자 정보 조회
        BusinessProfile businessProfile = businessProfileRepository.findByTargetIdAndTargetType(expoId, TargetType.EXPO)
                .orElse(null);

        // 카테고리 정보 조회 (여러 카테고리를 콤마로 구분)
        String categoryName = expo.getExpoCategories().stream()
                .map(expoCategory -> expoCategory.getCategory().getName())
                .reduce((first, second) -> first + ", " + second)
                .orElse("카테고리 없음");

        return memberExpoDetailMapper.toMemberExpoDetailResponse(expo, paymentInfo, tickets, businessProfile, categoryName);
    }

    @Override
    @Transactional
    public void cancelExpo(Long memberId, Long expoId) {

        Expo expo = expoRepository.findById(expoId)
                .orElseThrow(() -> new CustomException(CustomErrorCode.EXPO_NOT_FOUND));

        if (!expo.getMember().getId().equals(memberId)) {
            throw new CustomException(CustomErrorCode.EXPO_ACCESS_DENIED);
        }

        ExpoStatus oldStatus = expo.getStatus();

        switch (oldStatus) {

            case PENDING_APPROVAL -> {
                expo.updateStatus(ExpoStatus.CANCELLED);
                log.info("박람회 승인대기 취소 - 상태만 변경: 박람회 ID {}", expoId);
            }
            case PENDING_PAYMENT -> {
                expo.updateStatus(ExpoStatus.CANCELLED);

                expoPaymentInfoRepository.findByExpoId(expoId)
                        .ifPresent(paymentInfo -> {
                            expoPaymentInfoRepository.delete(paymentInfo);
                            log.info("박람회 결제대기 취소 - ExpoPaymentInfo 삭제됨: 박람회 ID {}", expoId);
                        });
            }
            default -> throw new CustomException(CustomErrorCode.INVALID_EXPO_STATUS);
        }

        ExpoStatus newStatus = expo.getStatus();

        notificationService.notifyExpoStatusChange(
                expo,
                oldStatus,
                newStatus
        );
    }


    @Override
    public ExpoPaymentDetailResponse getExpoPaymentDetail(Long memberId, Long expoId) {
        // 박람회 정보 조회
        Expo expo = expoRepository.findById(expoId)
                .orElseThrow(() -> new CustomException(CustomErrorCode.EXPO_NOT_FOUND));

        if (!expo.getMember().getId().equals(memberId)) {
            throw new CustomException(CustomErrorCode.EXPO_ACCESS_DENIED);
        }

        // 사업자 정보 조회
        BusinessProfile businessProfile = businessProfileRepository.findByTargetIdAndTargetType(expoId, TargetType.EXPO)
                .orElseThrow(() -> new CustomException(CustomErrorCode.BUSINESS_NOT_EXIST));

        // 박람회 결제 정보 조회
        ExpoPaymentInfo expoPaymentInfo = expoPaymentInfoRepository.findByExpoId(expoId)
                .orElseThrow(() -> new CustomException(CustomErrorCode.PAYMENT_INFO_NOT_FOUND));

        return expoPaymentDetailMapper.toExpoPaymentDetailResponse(expo, businessProfile, expoPaymentInfo);
    }

    @Override
    public List<ExpoAdminCodeResponse> getExpoAdminCodes(Long memberId, Long expoId) {
        // 박람회가 해당 회원의 것인지 확인
        Expo expo = expoRepository.findById(expoId)
                .orElseThrow(() -> new CustomException(CustomErrorCode.EXPO_NOT_FOUND));

        if (!expo.getMember().getId().equals(memberId)) {
            throw new CustomException(CustomErrorCode.EXPO_ACCESS_DENIED);
        }

        // 해당 박람회의 관리자 코드 5개 조회
        List<AdminCode> adminCodes = adminCodeRepository.findByExpoId(expoId);

        return expoAdminCodeMapper.toExpoAdminCodeResponseList(adminCodes);
    }

    @Override
    public ExpoSettlementReceiptResponse getExpoSettlementReceipt(Long memberId, Long expoId) {
        // 박람회가 해당 회원의 것인지 확인
        Expo expo = expoRepository.findById(expoId)
                .orElseThrow(() -> new CustomException(CustomErrorCode.EXPO_NOT_FOUND));

        if (!expo.getMember().getId().equals(memberId)) {
            throw new CustomException(CustomErrorCode.EXPO_ACCESS_DENIED);
        }

        // 해당 박람회의 티켓 목록 조회
        List<Ticket> tickets = ticketRepository.findByExpoId(expoId);
        
        // 해당 박람회의 CONFIRMED 예약 목록 조회 (정산용)
        List<Reservation> confirmedReservations = reservationRepository.findByExpoId(expoId).stream()
                .filter(reservation -> reservation.getStatus() == ReservationStatus.CONFIRMED)
                .toList();

        // 박람회 결제 정보 조회 (결제 시점의 수수료율 사용)
        ExpoPaymentInfo expoPaymentInfo = expoPaymentInfoRepository.findByExpoId(expoId)
                .orElseThrow(() -> new CustomException(CustomErrorCode.PAYMENT_INFO_NOT_FOUND));

        // SETTLEMENT_REQUESTED 또는 COMPLETED 상태일 때 정산 정보 조회
        Settlement settlement = null;
        if (expo.getStatus() == ExpoStatus.SETTLEMENT_REQUESTED || expo.getStatus() == ExpoStatus.COMPLETED) {
            settlement = settlementRepository.findByExpoId(expoId).orElse(null);
        }
        
        // Mapper에서 모든 정보를 한번에 처리 (reservations 추가)
        return expoSettlementReceiptMapper.toSettlementReceiptResponse(
                expo, tickets, confirmedReservations, expoPaymentInfo, settlement);
    }

    @Override
    public ExpoRefundReceiptResponse getExpoRefundReceipt(Long memberId, Long expoId) {
        // 박람회가 해당 회원의 것인지 확인
        Expo expo = expoRepository.findById(expoId)
                .orElseThrow(() -> new CustomException(CustomErrorCode.EXPO_NOT_FOUND));

        if (!expo.getMember().getId().equals(memberId)) {
            throw new CustomException(CustomErrorCode.EXPO_ACCESS_DENIED);
        }

        // 사업자 정보 조회
        BusinessProfile businessProfile = businessProfileRepository.findByTargetIdAndTargetType(expoId,
                        TargetType.EXPO)
                .orElseThrow(() -> new CustomException(CustomErrorCode.BUSINESS_NOT_EXIST));

        // 박람회 결제 정보 조회
        ExpoPaymentInfo expoPaymentInfo = expoPaymentInfoRepository.findByExpoId(expoId)
                .orElseThrow(() -> new CustomException(CustomErrorCode.PAYMENT_INFO_NOT_FOUND));

        // PENDING_CANCEL 상태인 경우 Refund 테이블 데이터 사용
        if (expo.getStatus() == ExpoStatus.PENDING_CANCEL) {
            log.info("PENDING_CANCEL 상태 감지 - expoId: {}, internal refund 조회 사용", expoId);

            // payment internal에서 환불 정보 조회
            RefundInternalResponse refund = refundInternalService.getRefundByTarget(
                    PaymentTargetType.EXPO, expoId);

            return expoRefundReceiptMapper.toRefundReceiptWithRefundData(
                    expo, businessProfile, expoPaymentInfo, refund);

        }
        
        log.info("기존 로직 사용 - expoId: {}, status: {}", expoId, expo.getStatus());

        return expoRefundReceiptMapper.toRefundReceiptDto(expo, businessProfile, expoPaymentInfo);
    }
    
    @Override
    public ExpoRefundReceiptResponse getExpoRefundHistory(Long memberId, Long expoId) {

        // 박람회가 해당 회원의 것인지 확인
        Expo expo = expoRepository.findById(expoId)
                .orElseThrow(() -> new CustomException(CustomErrorCode.EXPO_NOT_FOUND));

        if (!expo.getMember().getId().equals(memberId)) {
            throw new CustomException(CustomErrorCode.EXPO_ACCESS_DENIED);
        }

        // 환불 내역은 core DB 직접 조회 대신 payment internal에서 조회
        // - 환불 원천 검증을 internal에서 일관되게 수행
        // - core는 결과만 받아서 화면/응답용으로 사용
        RefundInternalResponse refund = refundInternalService.getRefundByTarget(
                PaymentTargetType.EXPO, expoId);

        // 박람회 결제 정보 조회
        ExpoPaymentInfo expoPaymentInfo = expoPaymentInfoRepository.findByExpoId(expoId)
                .orElseThrow(() -> new CustomException(CustomErrorCode.PAYMENT_INFO_NOT_FOUND));

        // 사업자 정보 조회
        BusinessProfile businessProfile = businessProfileRepository.findByTargetIdAndTargetType(expoId,
                        TargetType.EXPO)
                .orElseThrow(() -> new CustomException(CustomErrorCode.BUSINESS_NOT_EXIST));

        return expoRefundReceiptMapper.toRefundHistoryDto(expo, businessProfile, expoPaymentInfo, refund);
    }
    
    @Override
    @Transactional
    public void requestExpoSettlement(Long memberId, Long expoId, ExpoSettlementRequest request) {
        // 박람회가 해당 회원의 것인지 권한 확인
        Expo expo = expoRepository.findById(expoId)
                .orElseThrow(() -> new CustomException(CustomErrorCode.EXPO_NOT_FOUND));

        if (!expo.getMember().getId().equals(memberId)) {
            throw new CustomException(CustomErrorCode.EXPO_ACCESS_DENIED);
        }
        
        // Settlement 로직은 SettlementExpoAdminService로 완전 위임
        settlementExpoAdminService.requestSettlement(expoId, request);

        log.info("정산 신청 위임 완료 - 박람회 ID: {}, 회원 ID: {}", expoId, memberId);
    }

    @Override
    @Transactional
    public void completeExpoPayment(Long memberId, Long expoId) {

        // 박람회 조회 및 권한 확인
        Expo expo = expoRepository.findById(expoId)
                .orElseThrow(() -> new CustomException(CustomErrorCode.EXPO_NOT_FOUND));

        if (!expo.getMember().getId().equals(memberId)) {
            throw new CustomException(CustomErrorCode.EXPO_ACCESS_DENIED);
        }

        ExpoStatus oldStatus = expo.getStatus();

        // 1. 회원 역할을 EXPO_ADMIN으로 변경
        Member member = expo.getMember();
        member.updateRole(Role.EXPO_ADMIN);

        // 2. 박람회 상태를 PENDING_PUBLISH로 변경
        expo.updateStatus(ExpoStatus.PENDING_PUBLISH);

        // 3. ExpoPaymentInfo 상태를 PENDING에서 SUCCESS로 업데이트
        ExpoPaymentInfo paymentInfo = expoPaymentInfoRepository.findByExpoId(expoId)
                .orElseThrow(() -> new CustomException(CustomErrorCode.PAYMENT_INFO_NOT_FOUND));
        paymentInfo.updateStatus(PaymentStatus.SUCCESS);

        // 4. 5개의 EXPO_ADMIN_CODE 생성 및 권한 설정
        List<AdminCode> adminCodes = generateAdminCodes(expo, 5);
        adminCodeRepository.saveAll(adminCodes);

        // 5. 각 AdminCode에 대한 AdminPermission 생성 (모든 권한 true)
        List<AdminPermission> adminPermissions = new ArrayList<>();
        for (AdminCode adminCode : adminCodes) {
            AdminPermission permission = AdminPermission.builder()
                    .adminCode(adminCode)
                    .isExpoDetailUpdate(true)
                    .isBoothInfoUpdate(true)
                    .isScheduleUpdate(true)
                    .isReserverListView(true)
                    .isPaymentView(true)
                    .isEmailLogView(true)
                    .isOperationsConfigUpdate(false)
                    .isInquiryView(true)
                    .build();
            adminPermissions.add(permission);
        }
        adminPermissionRepository.saveAll(adminPermissions);

        ExpoStatus newStatus = expo.getStatus();

        notificationService.notifyExpoStatusChange(expo, oldStatus, newStatus);
    }

    /**
     * 관리자 코드 생성
     */
    private List<AdminCode> generateAdminCodes(Expo expo, int count) {
        List<AdminCode> adminCodes = new ArrayList<>();
        SecureRandom random = new SecureRandom();

        // expo의 displayEndDate + 30일로 만료시간 설정
        LocalDateTime expiredAt = expo.getDisplayEndDate().atStartOfDay().plusDays(30);

        for (int i = 0; i < count; i++) {
            String code = "CODE" + generateRandomCode(6, random);

            AdminCode adminCode = AdminCode.builder()
                    .expoId(expo.getId())
                    .code(code)
                    .expiredAt(expiredAt)
                    .build();

            adminCodes.add(adminCode);
        }

        return adminCodes;
    }

    /**
     * 랜덤 코드 생성 (숫자와 대문자 알파벳)
     */
    private String generateRandomCode(int length, SecureRandom random) {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder code = new StringBuilder();

        for (int i = 0; i < length; i++) {
            code.append(characters.charAt(random.nextInt(characters.length())));
        }

        return code.toString();
    }
}