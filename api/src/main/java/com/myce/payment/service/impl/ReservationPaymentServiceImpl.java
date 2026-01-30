package com.myce.payment.service.impl;

import com.myce.client.payment.service.PaymentInternalService;
import com.myce.common.exception.CustomErrorCode;
import com.myce.common.exception.CustomException;
import com.myce.expo.entity.Expo;
import com.myce.expo.entity.Ticket;
import com.myce.expo.repository.ExpoRepository;
import com.myce.expo.repository.TicketRepository;
import com.myce.expo.service.info.TicketService;
import com.myce.payment.dto.*;
import com.myce.payment.entity.ReservationPaymentInfo;
import com.myce.payment.entity.type.PaymentStatus;
import com.myce.payment.entity.type.PaymentTargetType;
import com.myce.payment.repository.ReservationPaymentInfoRepository;
import com.myce.payment.service.ReservationPaymentService;
import com.myce.payment.service.mapper.PaymentMapper;
import com.myce.reservation.dto.GuestReservationRequest;
import com.myce.reservation.dto.PreReservationCacheDto;
import com.myce.reservation.dto.ReserverBulkSaveRequest;
import com.myce.reservation.dto.ReserverInfo;
import com.myce.reservation.entity.Reservation;
import com.myce.reservation.entity.code.ReservationStatus;
import com.myce.reservation.entity.code.UserType;
import com.myce.reservation.repository.ReservationRepository;
import com.myce.reservation.service.GuestReservationService;
import com.myce.reservation.service.ReservationService;
import com.myce.reservation.service.ReserverService;
import java.util.List;
import java.util.Objects;

// import com.myce.client.payment.PaymentInternalClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationPaymentServiceImpl implements ReservationPaymentService {
    private final ReservationPaymentInfoRepository reservationPaymentInfoRepository;
    private final ReservationRepository reservationRepository;
    // private final PreReservationRepository preReservationRepository; -> 이제 repository에서 안함(G1)
    private final ExpoRepository expoRepository;
    private final TicketRepository ticketRepository;
    private final PaymentMapper paymentMapper;
    private final ReservationService reservationService;
    private final ReserverService reserverService;
    private final TicketService ticketService;
    private final PaymentCommonService paymentCommonService;
    private final GuestReservationService guestReservationService;
    // private final PaymentInternalClient paymentClientService; // Payment 내부 API 호출 전담
    private final PaymentInternalService paymentInternalClient;

    @Override
    @Transactional
    public PaymentVerifyResponse verifyAndCompleteReservationPayment(ReservationPaymentVerifyRequest request,
                                                                     PreReservationCacheDto cacheDto) {
        //cacheDto 추가
        log.info("박람회 결제 통합 처리 시작 - reservationId: {}", request.getTargetId());

        // 2. Redis에서 결제 세션 검증 및 DB 저장 => Redis에서 결제 세션 검증 (세션 ID 필수)
        //String sessionId = request.getSessionId(); -> sessionId 안써요~~
        Reservation reservation = saveReservation(cacheDto); // sessionId -> cacheDto
        UserType userType = reservation.getUserType();
        long reservationId = reservation.getId();
        int paidAmount = request.getAmount();

        // 3. 비회원 적립금 지급 방지
        if (userType.equals(UserType.GUEST)) request.setSavedMileage(0);

        try {
            // 4. ✅ payment 서버로 내부 호출 -> internal
            // - 포트원 검증 + Payment 저장을 payment 서버가 처리하도록 위임
            PaymentInternalRequest internalRequest = PaymentInternalRequest.builder()
                    .impUid(request.getImpUid())
                    .merchantUid(request.getMerchantUid())
                    .amount(request.getAmount())
                    .reservationId(reservationId)
                    .targetType(PaymentTargetType.RESERVATION)
                    .targetId(reservationId)
                    .build();
            // payment 내부 API 호출 (응답 DTO 수신) ->TODO  상태값만 체크 post랑 get 중에 DTO 바로 넘겨도 되는지 해도 상관 없는지 check -
            //            ResponseEntity<PaymentInternalResponse>  internalResponse = // http 상태값 받아올수 있다
            //                    paymentClientService.post(
            //                            //TODO API 이름
            //                            "/payment",
            //                            internalRequest,
            //                            PaymentInternalResponse.class);
            PaymentInternalResponse body = paymentInternalClient.verifyAndSave(internalRequest);
            //            //controller -> responseentity  받을 때 여기서 상태에 따라 예외 처리 -> 상태 체크
            //            if(!internalResponse.getStatusCode().equals(HttpStatus.OK)){
            //                log.warn("payment internal 실패 - status {}",internalResponse.getStatusCode());
            //                throw new CustomException(CustomErrorCode.PAYMENT_NOT_PAID);
            //            }
            //            //body NULL 체크
            //            PaymentInternalResponse body= internalResponse.getBody();
            //            if (body == null) {
            //                throw new CustomException(CustomErrorCode.INTERNAL_SERVER_ERROR);
            //            }

            // 5. ReservationPaymentInfo 저장
            PaymentVerifyInfo verifyInfo = convertToPaymentVerifyInfo(request, reservationId);
            ReservationPaymentInfo paymentInfo =
                    saveReservationPayment(verifyInfo, reservation, paidAmount, PaymentStatus.SUCCESS);

            // 6. 예약 상태를 CONFIRMED로 변경
            reservationService.updateStatusToConfirm(reservationId);

            // 7. 예약자 정보 저장 및 비회원 Guest ID 생성
            if (request.getReserverInfos() != null && !request.getReserverInfos().isEmpty()) {
                saveReservers(reservationId, request.getReserverInfos(), userType);
            }

            // 8. 티켓 수량 감소
            if (request.getTicketId() != null && request.getQuantity() != null) {
                ticketService.updateRemainingQuantity(request.getTicketId(), request.getQuantity());
            }

            // 9. 마일리지 처리 & 회원 등급 업데이트(회원만)
            Long userId = reservation.getUserId();
            if (userType.equals(UserType.MEMBER)) {
                // 가상계좌 입금 완료 시 마일리지 및 등급 업데이트 (회원만)
                int usedMileage = Objects.requireNonNullElse(request.getUsedMileage(), 0);
                int savedMileage = Objects.requireNonNullElse(request.getSavedMileage(), 0);
                paymentCommonService.processMileage(usedMileage, savedMileage, userId);
            }

            // 11. QR 코드 생성 시도 (실패해도 계속 진행)
            paymentCommonService.issueQrForReservation(reservation);

            // 12. 결제 완료 알림 발송
            // 회원: 기존 사이트 내 알림도 유지
            if (userType.equals(UserType.MEMBER)) {
                paymentCommonService.sendAlert(reservation, paidAmount);
            }

            // 회원/비회원 공통: 이메일 알림 (첫 번째 예약자에게만 발송)
            if (request.getReserverInfos() != null && !request.getReserverInfos().isEmpty()) {
                ReserverBulkSaveRequest.ReserverSaveInfo reserverInfo = request.getReserverInfos().getFirst();
                paymentCommonService.sendEmail(reservation, reserverInfo, paidAmount);
            }

            // 13. Redis에서 결제 세션 정리 -> 삭제!

            log.info("박람회 결제 통합 처리 완료 - reservationId: {}", reservation.getId());

            // ✅ 응답은 payment 내부 응답 기반으로 반환
            return PaymentVerifyResponse.builder()
                    .impUid(body.getImpUid())
                    .merchantUid(body.getMerchantUid())
                    .status(paymentInfo.getStatus().name())
                    .amount(paidAmount)
                    .reservationId(body.getReservationId())
                    .build();
        } catch (Exception e) {
            log.error("박람회 결제 통합 처리 실패 - 오류: {}", e.getMessage(), e);
            throw new CustomException(CustomErrorCode.PAYMENT_NOT_PAID);
        }
    }

    @Override
    @Transactional
    public PaymentVerifyResponse verifyAndPendingVbankReservationPayment(ReservationPaymentVerifyRequest request,
                                                                         PreReservationCacheDto cacheDto) {
        log.info("박람회 가상계좌 결제 통합 처리 시작 - reservationId: {}", request.getTargetId());

        // 1) Redis에 있던 사전 예약 데이터를 DB에 저장 -> 실제 reservationId 생성
        Reservation reservation = saveReservation(cacheDto);
        long reservationId = reservation.getId();

        // 2) 결제 금액 (요청 기준)
        int paidAmount = request.getAmount();

        // 3) 비회원 적립금 방지 (비회원은 savedMileage=0)
        if (reservation.getUserType().equals(UserType.GUEST)) {
            request.setSavedMileage(0);
        }

        try {
            // 4) payment 내부 API 호출 (vbank 전용)
            PaymentInternalRequest internalRequest = PaymentInternalRequest.builder()
                    .impUid(request.getImpUid())
                    .merchantUid(request.getMerchantUid())
                    .amount(paidAmount)
                    .reservationId(reservationId)
                    .targetType(PaymentTargetType.RESERVATION)
                    .targetId(reservationId)
                    .build();

            PaymentInternalResponse body = paymentInternalClient.verifyAndSaveVbank(internalRequest);

            // 6) 코어에서 ReservationPaymentInfo 저장 (PENDING)
            PaymentVerifyInfo verifyInfo = convertToPaymentVerifyInfo(request, reservationId);
            ReservationPaymentInfo paymentInfo =
                    saveReservationPayment(verifyInfo, reservation, paidAmount, PaymentStatus.PENDING);

            // 7) 예약자 저장 (있을 때만)
            if (request.getReserverInfos() != null && !request.getReserverInfos().isEmpty()) {
                saveReservers(reservationId, request.getReserverInfos(), reservation.getUserType());
            }

            // 8) 티켓 수량 감소
            if (request.getTicketId() != null && request.getQuantity() != null) {
                ticketService.updateRemainingQuantity(request.getTicketId(), request.getQuantity());
            }

            // 9) vbank는 입금 완료 시 웹훅에서 후속 처리됨
            log.info("박람회 가상계좌 결제 처리 완료 - reservationId: {}", reservationId);

            // 10) 응답 (결제는 PENDING 상태)
            return PaymentVerifyResponse.builder()
                    .impUid(body.getImpUid())
                    .merchantUid(body.getMerchantUid())
                    .status(paymentInfo.getStatus().name())  // PENDING
                    .amount(paidAmount)
                    .reservationId(body.getReservationId())
                    .build();

        } catch (Exception e) {
            log.error("박람회 가상계좌 결제 처리 실패 - reservationId: {}", reservationId, e);
            throw new CustomException(CustomErrorCode.PAYMENT_NOT_READY_OR_PAID);
        }
    }

    private PaymentVerifyInfo convertToPaymentVerifyInfo(
            ReservationPaymentVerifyRequest request, Long actualReservationId) {
        PaymentVerifyInfo verifyInfo = new PaymentVerifyInfo();
        verifyInfo.setImpUid(request.getImpUid());
        verifyInfo.setMerchantUid(request.getMerchantUid());
        verifyInfo.setAmount(request.getAmount());
        verifyInfo.setTargetType(request.getTargetType());
        verifyInfo.setTargetId(actualReservationId); // 실제 DB에 저장된 reservation ID 사용
        verifyInfo.setUsedMileage(request.getUsedMileage());
        verifyInfo.setSavedMileage(request.getSavedMileage());
        return verifyInfo;
    }

    private Reservation getNewReservation(PreReservationCacheDto PreReservationInfo) {
        Expo expo = expoRepository.findById(PreReservationInfo.getExpoId())
                .orElseThrow(() -> new CustomException(CustomErrorCode.EXPO_NOT_EXIST));
        Ticket ticket = ticketRepository.findById(PreReservationInfo.getTicketId())
                .orElseThrow(() -> new CustomException(CustomErrorCode.TICKET_NOT_EXIST));

        return Reservation.builder()
                .expo(expo)
                .ticket(ticket)
                .reservationCode(PreReservationInfo.getReservationCode())
                .userType(PreReservationInfo.getUserType())
                .userId(PreReservationInfo.getUserId())
                .quantity(PreReservationInfo.getQuantity())
                .status(ReservationStatus.CONFIRMED_PENDING)
                .build();
    }

    // saveReservationToSessionId() → saveReservation() 변경 -> Redis 조회 제거 및 cacheDto를 파라미터로
    private Reservation saveReservation(PreReservationCacheDto cacheDto) {
        Reservation newReservation = getNewReservation(cacheDto);
        return reservationRepository.save(newReservation);
    }

    private ReservationPaymentInfo saveReservationPayment(
            PaymentVerifyInfo verifyInfo, Reservation reservation, int paidAmount, PaymentStatus paymentStatus) {
        int usedMileage = verifyInfo.getUsedMileage();
        int savedMileage = verifyInfo.getSavedMileage();
        ReservationPaymentInfo paymentInfo = paymentMapper.toReservationPaymentInfo(
                reservation, paidAmount, paymentStatus, usedMileage, savedMileage);
        return reservationPaymentInfoRepository.save(paymentInfo);
    }

    private void saveReservers(long reservationId,
                               List<ReserverBulkSaveRequest.ReserverSaveInfo> reserverInfos, UserType userType) {
        reserverService.saveReservers(reservationId, reserverInfos);

        // 비회원인 경우 Guest 엔티티 생성 및 reservation의 userId 업데이트
        if (userType.equals(UserType.GUEST)) {
            GuestReservationRequest guestRequest = GuestReservationRequest.builder()
                    .reservationId(reservationId)
                    .reserverInfos(convertReserverInfoList(reserverInfos))
                    .build();
            guestReservationService.updateGuestId(guestRequest);
            log.info("비회원 Guest ID 생성 및 업데이트 완료 - reservationId: {}", reservationId);
        }
    }

    private List<ReserverInfo> convertReserverInfoList(List<ReserverBulkSaveRequest.ReserverSaveInfo> reserverInfos) {
        return reserverInfos.stream().parallel()
                .map(info -> ReserverInfo.builder()
                        .name(info.getName())
                        .email(info.getEmail())
                        .phone(info.getPhone())
                        .phone(info.getPhone())
                        .birth(info.getBirth())
                        .gender(info.getGender())
                        .build()).collect(java.util.stream.Collectors.toList());
    }
    // destroyPreReservation() 삭제
}
