package com.myce.qrcode.service.impl;

import com.myce.auth.dto.type.LoginType;
import com.myce.common.exception.CustomErrorCode;
import com.myce.common.exception.CustomException;
import com.myce.expo.entity.AdminCode;
import com.myce.expo.entity.Expo;
import com.myce.expo.repository.AdminCodeRepository;
import com.myce.client.notification.service.NotificationService;
import com.myce.qrcode.dto.QrUseResponse;
import com.myce.qrcode.dto.QrVerifyResponse;
import com.myce.qrcode.entity.QrCode;
import com.myce.qrcode.entity.code.QrCodeStatus;
import com.myce.qrcode.repository.QrCodeRepository;
import com.myce.qrcode.service.QrCodeService;
import com.myce.qrcode.service.QrCodeGenerateService;
import com.myce.qrcode.service.QrNotificationService;
import com.myce.qrcode.service.mapper.QrResponseMapper;
import com.myce.reservation.entity.Reserver;
import com.myce.reservation.entity.Reservation;
import com.myce.reservation.entity.code.UserType;
import com.myce.reservation.repository.ReservationRepository;
import com.myce.reservation.repository.ReserverRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class QrCodeServiceImpl implements QrCodeService {

    private final QrCodeRepository qrCodeRepository;
    private final ReserverRepository reserverRepository;
    private final ReservationRepository reservationRepository;
    private final AdminCodeRepository adminCodeRepository;
    private final QrResponseMapper qrResponseMapper;
    private final QrCodeGenerateService qrCodeGenerateService;
    private final QrNotificationService qrNotificationService;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public void issueQr(Long reserverId) {
        log.info("QR 코드 발급 시작 - 예약자 ID: {}", reserverId);

        Reserver reserver = reserverRepository.findById(reserverId)
                .orElseThrow(() -> new CustomException(CustomErrorCode.RESERVER_NOT_FOUND));

        if (qrCodeRepository.findByReserver(reserver).isPresent()) {
            log.debug("[issueQr] 중복 발급 요청 차단 - 이미 QR 존재. reserverId={}", reserverId);
            throw new CustomException(CustomErrorCode.QR_ALREADY_EXISTS);
        }

        QrCode qrCode = qrCodeGenerateService.createQrCode(reserver);
        qrCodeRepository.save(qrCode);
        log.info("QR 코드 발급 완료 - 예약자 ID: {}", reserverId);
        
        // 알림 전송 실패는 트랜잭션 롤백 안되는 걸로 진행
        qrNotificationService.sendQrIssuedNotification(reserver, false);

    };

    @Override
    @Transactional
    public void issueQrWithoutNotification(Long reserverId) {
        log.info("QR 코드 발급 시작 (알림 없음) - 예약자 ID: {}", reserverId);

        Reserver reserver = reserverRepository.findById(reserverId)
                .orElseThrow(() -> new CustomException(CustomErrorCode.RESERVER_NOT_FOUND));

        if (qrCodeRepository.findByReserver(reserver).isPresent()) {
            throw new CustomException(CustomErrorCode.QR_ALREADY_EXISTS);
        }

        QrCode qrCode = qrCodeGenerateService.createQrCode(reserver);
        qrCodeRepository.save(qrCode);
    }

    @Override
    @Transactional
    public void reissueQr(Long reserverId, Long adminMemberId, LoginType loginType) {
        log.info("QR 코드 재발급 시작 - 예약자 ID: {}, 관리자 ID: {}", reserverId, adminMemberId);

        Reserver reserver = reserverRepository.findById(reserverId)
                .orElseThrow(() -> new CustomException(CustomErrorCode.RESERVER_NOT_FOUND));

        validateAdminPermission(adminMemberId, reserver, loginType);

        QrCode existing = qrCodeRepository.findByReserver(reserver)
                .orElseThrow(() -> new CustomException(CustomErrorCode.QR_NOT_FOUND));

        if (existing.getStatus() != QrCodeStatus.ACTIVE && existing.getStatus() != QrCodeStatus.APPROVED) {
            throw new CustomException(CustomErrorCode.QR_INVALID_STATUS);
        }

        log.info("기존 QR 코드 삭제 처리 - QR ID: {}", existing.getId());
        qrCodeRepository.delete(existing);
        qrCodeRepository.flush();

        QrCode qrCode = qrCodeGenerateService.createQrCode(reserver);
        qrCodeRepository.save(qrCode);
        log.info("QR 코드 재발급 완료 - 예약자 ID: {}", reserverId);

        // QR 재발급 성공 알림 전송 (try-catch로 예외 허용)
        qrNotificationService.sendQrIssuedNotification(reserver, true);

    }

    @Override
    @Transactional
    public QrUseResponse updateQrAsUsed(String qrToken, Long adminMemberId, LoginType loginType) {
        log.info("QR 코드 사용 처리 시작 - 토큰: {}, 관리자 ID: {}, 로그인 타입: {}", qrToken, adminMemberId, loginType);

        QrCode qr = qrCodeRepository.findByQrToken(qrToken)
                .orElseThrow(() -> new CustomException(CustomErrorCode.QR_NOT_FOUND));

        validateAdminPermission(adminMemberId, qr.getReserver(), loginType);

        // ACTIVE인 경우만 상태 변경
        boolean wasActive = qr.getStatus() == QrCodeStatus.ACTIVE;

        if (wasActive) {
            qr.markAsUsed();
        }
        log.info("QR 코드 사용 처리 완료 - QR ID: {}, 예약자 ID: {}, 사용처리됨: {}",
                qr.getId(), qr.getReserver().getId(), wasActive);

        return qrResponseMapper.toUseResponse(qr, wasActive);
    }

    @Override
    @Transactional(readOnly = true)
    public String getQrImageUrlByReserverId(Long reserverId) {
        log.info("예약자 ID로 QR 이미지 URL 조회 - 예약자 ID: {}", reserverId);

        Reserver reserver = reserverRepository.findById(reserverId)
                .orElseThrow(() -> new CustomException(CustomErrorCode.RESERVER_NOT_FOUND));

        QrCode qrCode = qrCodeRepository.findByReserver(reserver)
                .orElseThrow(() -> new CustomException(CustomErrorCode.QR_NOT_FOUND));

        return qrCode.getQrImageUrl();
    }

    @Override
    @Transactional(readOnly = true)
    public String getQrImageUrlByToken(String token) {
        log.info("토큰으로 QR 이미지 URL 조회 - 토큰: {}", token);

        QrCode qrCode = qrCodeRepository.findByQrToken(token)
                .orElseThrow(() -> new CustomException(CustomErrorCode.QR_NOT_FOUND));

        return qrCode.getQrImageUrl();
    }

    private void validateAdminPermission(Long adminId, Reserver reserver, LoginType loginType) {
        Long expoId = reserver.getReservation().getExpo().getId();

        switch (loginType) {
            case MEMBER -> {
                // Member 테이블의 PK로 전시회 소유자 확인
                Long managerId = reserver.getReservation()
                        .getExpo()
                        .getMember()
                        .getId();
                if (!managerId.equals(adminId)) {
                    throw new CustomException(CustomErrorCode.QR_UNAUTHORIZED);
                }
            }
            case ADMIN_CODE -> {
                // AdminCode 테이블의 PK로 전시회 권한 확인
                AdminCode adminCode = adminCodeRepository.findById(adminId)
                        .orElseThrow(() -> new CustomException(CustomErrorCode.ADMIN_CODE_NOT_FOUND));
                if (!adminCode.getExpoId().equals(expoId)) {
                    throw new CustomException(CustomErrorCode.QR_UNAUTHORIZED);
                }
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public QrVerifyResponse verifyQrCode(String token, Long adminMemberId, LoginType loginType) {
        log.info("QR 코드 검증 시작 - token: {}, adminId: {}, 로그인 타입: {}", token, adminMemberId, loginType);

        QrCode qrCode = qrCodeRepository.findByQrToken(token)
                .orElseThrow(() -> new CustomException(CustomErrorCode.QR_NOT_FOUND));

        // 관리자 권한 검증
        validateAdminPermission(adminMemberId, qrCode.getReserver(), loginType);

        // 매퍼를 통해 응답 생성 (상태만 체크)
        QrVerifyResponse response = qrResponseMapper.toVerifyResponse(qrCode);

        log.info("QR 코드 검증 완료 - token: {}, 상태: {}, 유효성: {}",
                token, qrCode.getStatus(), response.isValid());
        return response;
    }

    @Override
    @Transactional
    public void issueQrForReservation(Long reservationId) {
        log.info("예매 완료 시 QR 코드 즉시 생성 시작 - 예약 ID: {}", reservationId);

        // 해당 예약의 모든 예약자 조회
        List<Reserver> reservers = reserverRepository.findByReservationId(reservationId);

        if (reservers.isEmpty()) {
            log.warn("예약자를 찾을 수 없습니다 - 예약 ID: {}", reservationId);
            return;
        }

        Reservation reservation = reservationRepository.findById(reservationId)
            .orElseThrow(() -> new CustomException(CustomErrorCode.RESERVATION_NOT_FOUND));

        Expo expo = reservation.getExpo();
        LocalDate today = LocalDate.now();
        LocalDate expoStartDate = expo.getStartDate();
        LocalDate twoDaysBefore = expoStartDate.minusDays(2);

        // 박람회 시작 2일 전부터 즉시 QR 생성 (스케줄러 백업)
        if (today.isAfter(twoDaysBefore) || today.isEqual(twoDaysBefore)) {
            log.info("박람회 시작 2일 전 이후 예매 감지 - 즉시 QR 생성. 박람회: {}, 시작일: {}, 오늘: {}",
                    expo.getTitle(), expoStartDate, today);

            int successCount = 0;
            int failCount = 0;
            boolean shouldSendNotification = false;

            // 1단계: 모든 reserver에 대해 QR 생성
            for (Reserver reserver : reservers) {
                try {
                    // 기존 QR이 있는지 확인
                    if (qrCodeRepository.findByReserver(reserver).isPresent()) {
                        log.debug("이미 QR 코드가 존재함 - 예약자 ID: {}", reserver.getId());
                        continue;
                    }

                    // 날짜에 따라 적절한 상태로 QR 코드 생성
                    QrCode qrCode = qrCodeGenerateService.createQrCode(reserver);
                    qrCodeRepository.save(qrCode);
                    log.debug("즉시 QR 코드 생성 완료 - 예약자 ID: {}", reserver.getId());

                    successCount++;

                } catch (Exception e) {
                    log.error("즉시 QR 코드 생성 실패 - 예약자 ID: {}, 오류: {}",
                            reserver.getId(), e.getMessage(), e);
                    failCount++;
                }
            }

            // 2단계: QR 생성이 성공한 경우에만 예약별 알림 전송 (1회)
            // 알림 전송 실패해도 진행한다.
            if (successCount >= 1 && reservation.getUserType() == UserType.MEMBER) {
                try {
                    Long memberId = reservation.getUserId();
                    String expoTitle = reservation.getExpo().getTitle();

                    notificationService.sendQrIssuedNotification(memberId, reservationId, expoTitle);

                    log.info("QR 발급 알림 처리 완료 - 예약 ID: {}, 회원 ID: {}", reservationId, memberId);
                } catch (Exception e) {
                    log.error( "QR 발급 알림 처리 실패 - 예약 ID: {}, 오류: {}", reservationId, e.getMessage(), e );
                }
            }

            log.info("예매 완료 시 QR 코드 즉시 생성 완료 - 예약 ID: {}, 성공: {} 명, 실패: {} 명",
                    reservationId, successCount, failCount);
        } else {
            log.info("박람회 시작까지 2일 이상 남음 - 스케줄러에서 생성 예정. 박람회: {}, 시작일: {}, 오늘: {}",
                    expo.getTitle(), expoStartDate, today);
        }
    }
}
