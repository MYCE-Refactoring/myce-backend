package com.myce.advertisement.service.impl;

import com.myce.advertisement.dto.AdRegistrationRequest;
import com.myce.advertisement.dto.AdRejectInfoResponse;
import com.myce.advertisement.entity.type.AdvertisementStatus;
import com.myce.advertisement.service.AdSystemService;
import com.myce.advertisement.service.mapper.*;
import com.myce.client.notification.service.NotificationService;
import com.myce.client.payment.service.RefundInternalService;
import com.myce.common.entity.RejectInfo;
import com.myce.common.repository.RejectInfoRepository;
import com.myce.member.dto.ad.*;
import com.myce.payment.dto.RefundInternalRequest;
import com.myce.payment.dto.RefundInternalResponse;
import com.myce.payment.entity.AdPaymentInfo;
import com.myce.payment.entity.type.PaymentStatus;
import com.myce.payment.entity.type.PaymentTargetType;
import com.myce.payment.repository.AdPaymentInfoRepository;
import com.myce.system.entity.AdPosition;
import com.myce.advertisement.entity.Advertisement;
import com.myce.system.repository.AdPositionRepository;
import com.myce.advertisement.repository.AdRepository;
import com.myce.advertisement.service.AdUserService;
import com.myce.common.dto.RegistrationCompanyRequest;
import com.myce.common.entity.BusinessProfile;
import com.myce.common.entity.type.TargetType;
import com.myce.common.exception.CustomErrorCode;
import com.myce.common.exception.CustomException;
import com.myce.common.repository.BusinessProfileRepository;
import com.myce.common.service.mapper.BusinessProfileMapper;
import com.myce.member.entity.Member;
import com.myce.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class AdUserServiceImpl implements AdUserService {

  private final MemberRepository memberRepository;
  private final AdPositionRepository adPositionRepository;
  private final AdRepository adRepository;
  private final RejectInfoRepository rejectInfoRepository;
  private final BusinessProfileRepository  businessProfileRepository;
  private final AdPaymentInfoRepository adPaymentInfoRepository;

  private final MemberAdvertisementMapper memberAdvertisementMapper;
  private final AdvertisementDetailMapper advertisementDetailMapper;
  private final AdvertisementPaymentDetailMapper advertisementPaymentDetailMapper;
  private final AdvertisementRefundReceiptMapper advertisementRefundReceiptMapper;
  private final AdRefundReceiptMapper adRefundReceiptMapper;
  private final AdStatusValidateComponent adStatusValidateComponent;


  private final AdSystemService adSystemService;
  private final NotificationService notificationService;
  private final RefundInternalService refundInternalService;


  @Override
  @Transactional
  public void saveAdvertisement(Long memberId, AdRegistrationRequest request) {
    // 로그인한 사용자
    Member member = memberRepository.findById(memberId)
        .orElseThrow(() -> new CustomException(CustomErrorCode.MEMBER_NOT_EXIST));

    // 광고 위치
    AdPosition adPosition = adPositionRepository.findById(request.getAdPositionId())
        .orElseThrow(() -> new CustomException(CustomErrorCode.AD_POSITION_NOT_EXIST));

    // 총 등록일 수 구하기
    LocalDate startDate = request.getDisplayStartDate();
    LocalDate endDate = request.getDisplayEndDate();
    long totalDays = ChronoUnit.DAYS.between(startDate, endDate) + 1;

    // 광고 객체 생성
    Advertisement advertisement = AdRegistrationMapper.toEntity(request, member, adPosition, (int)totalDays);

    // 광고 등록(저장)
    adRepository.save(advertisement);

    // 등록 신청한 회사 정보 저장
    RegistrationCompanyRequest company = request.getRegistrationCompanyRequest();

    BusinessProfile businessProfile = BusinessProfileMapper.toEntity(company, TargetType.ADVERTISEMENT, advertisement.getId());
    businessProfileRepository.save(businessProfile);
  }

  @Override
  public Page<MemberAdvertisementResponse> getMemberAdvertisements(Long memberId, Pageable pageable) {
    Page<Advertisement> advertisements = adRepository.findByMemberIdWithAdPosition(memberId, pageable);
    return advertisements.map(memberAdvertisementMapper::toResponseDto);
  }

  @Override
  public AdvertisementDetailResponse getAdvertisementDetail(Long memberId, Long advertisementId) {

    Advertisement advertisement = adRepository.findByIdAndMemberIdWithAdPosition(advertisementId,
                    memberId)
            .orElseThrow(() -> new CustomException(CustomErrorCode.AD_NOT_FOUND));

    BusinessProfile businessProfile = businessProfileRepository.findByTargetIdAndTargetType(advertisementId,
                    TargetType.ADVERTISEMENT)
            .orElseThrow(() -> new CustomException(CustomErrorCode.BUSINESS_NOT_EXIST));

    return advertisementDetailMapper.toResponseDto(advertisement, businessProfile);
  }

  @Override
  @Transactional
  public void cancelAdvertisement(Long memberId, Long advertisementId) {
    Advertisement advertisement = adRepository.findByIdAndMemberIdWithAdPosition(advertisementId,
                    memberId)
            .orElseThrow(() -> new CustomException(CustomErrorCode.AD_NOT_FOUND));
    // 상태 검증
    adStatusValidateComponent.verifyCancel(advertisement);

    advertisement.cancel();

  }


  @Override
  @Transactional
  public void cancelByStatus(Long memberId, Long advertisementId) {

    Advertisement advertisement = adRepository
            .findByIdAndMemberIdWithAdPosition(advertisementId, memberId)
            .orElseThrow(() -> new CustomException(CustomErrorCode.AD_NOT_FOUND));

    AdvertisementStatus oldStatus = advertisement.getStatus();

    //상태 검증
    adStatusValidateComponent.verifyCancelByStatus(advertisement);


    switch (oldStatus) {

      case PENDING_APPROVAL -> {
        advertisement.updateStatus(AdvertisementStatus.CANCELLED);
      }

      case PENDING_PAYMENT, PENDING_PUBLISH -> {
        advertisement.updateStatus(AdvertisementStatus.CANCELLED);

        adPaymentInfoRepository.findByAdvertisementId(advertisementId)
                .ifPresent(adPaymentInfo -> {
                  adPaymentInfoRepository.delete(adPaymentInfo);
                  log.info("광고 취소 - AdPaymentInfo 삭제됨: 광고 ID {}", advertisementId);
                });
      }

      default -> {
        advertisement.cancelByStatus();
      }
    }

    AdvertisementStatus newStatus = advertisement.getStatus();

    // ✅ 알림은 단 한 번, enum 기반으로
    notificationService.notifyAdStatusChange(
            advertisement,
            oldStatus,
            newStatus
    );

  }



  @Override
  @Transactional
  public void requestRefundByStatus(Long memberId, Long advertisementId, AdRefundRequest request) {

    Advertisement advertisement = adRepository.findByIdAndMemberIdWithAdPosition( advertisementId, memberId ).orElseThrow( () -> new CustomException( CustomErrorCode.AD_NOT_FOUND ) );

    // AdPaymentInfo 조회
    AdPaymentInfo adPaymentInfo = adPaymentInfoRepository.findByAdvertisementId( advertisementId ).orElseThrow( () -> new CustomException( CustomErrorCode.PAYMENT_INFO_NOT_FOUND ) );

    try {
      refundInternalService.getRefundByTarget( PaymentTargetType.AD, advertisementId );
      // 환불 내역이 있으면 중복 신청으로 처리
      throw new CustomException(CustomErrorCode.ALREADY_REFUND_REQUESTED);
    } catch (CustomException e) {
      // 환불 내역이 없는 경우만 정상 흐름 진행
      if (e.getErrorCode() != CustomErrorCode.REFUND_NOT_FOUND) {
        throw e;
      }

      adStatusValidateComponent.verifyRequestRefundByStatus( advertisement );

      // 광고 상태에 따른 처리
      AdvertisementStatus oldStatus = advertisement.getStatus();

      advertisement.requestRefundByStatus();

      Integer refundAmount;

      switch (oldStatus) {
        case PENDING_PUBLISH:
          // 게시 예정 - 전액 환불
          refundAmount = adPaymentInfo.getTotalAmount();

          break;
        case PUBLISHED:
          // 게시 중 - 부분 환불 (남은 일수만큼)
          LocalDate today = LocalDate.now();
          LocalDate startDate = advertisement.getDisplayStartDate();

          // 영수증과 동일한 계산 방식: 사용한 일수 계산 후 차감
          int usedDays = (int) ChronoUnit.DAYS.between( startDate, today ) + 1; // +1은 시작일 포함
          if (usedDays < 0) usedDays = 0;

          // 남은 일수 계산
          int remainingDays = adPaymentInfo.getTotalDay() - usedDays;
          if (remainingDays < 0) remainingDays = 0;

          refundAmount = remainingDays * adPaymentInfo.getFeePerDay();
          break;
        default:
          throw new CustomException( CustomErrorCode.INVALID_ADVERTISEMENT_STATUS );
      }

      // 환불 신청은 payment internal에서 PENDING으로 생성
      String impUid = refundInternalService.getImpUid( PaymentTargetType.AD, advertisementId );
      RefundInternalRequest internalRequest = RefundInternalRequest.builder().impUid( impUid ).cancelAmount( refundAmount ).reason( request.getReason() ).build();

      refundInternalService.requestRefund(internalRequest);

      AdvertisementStatus newStatus = advertisement.getStatus();

      notificationService.notifyAdStatusChange( advertisement, oldStatus, newStatus );
    }
  }

  @Override
  public AdvertisementPaymentDetailResponse getAdvertisementPaymentDetail(Long memberId, Long advertisementId) {
    // 광고 정보 조회
    Advertisement advertisement = adRepository.findByIdAndMemberIdWithAdPosition(advertisementId,
                    memberId)
            .orElseThrow(() -> new CustomException(CustomErrorCode.AD_NOT_FOUND));

    // 사업자 정보 조회
    BusinessProfile businessProfile = businessProfileRepository.findByTargetIdAndTargetType(advertisementId,
                    TargetType.ADVERTISEMENT)
            .orElseThrow(() -> new CustomException(CustomErrorCode.BUSINESS_NOT_EXIST));

    // 광고 결제 정보 조회 (AdPaymentInfo 테이블에서)
    AdPaymentInfo adPaymentInfo = adPaymentInfoRepository.findByAdvertisementId(advertisementId)
            .orElseThrow(() -> new CustomException(CustomErrorCode.PAYMENT_INFO_NOT_FOUND));

    return advertisementPaymentDetailMapper.toAdvertisementPaymentDetailResponse(advertisement, businessProfile, adPaymentInfo);
  }

  @Override
  public AdvertisementRefundReceiptResponse getAdvertisementRefundReceipt(Long memberId, Long advertisementId) {
    // 광고 정보 조회
    Advertisement advertisement = adRepository.findByIdAndMemberIdWithAdPosition(advertisementId,
                    memberId)
            .orElseThrow(() -> new CustomException(CustomErrorCode.AD_NOT_FOUND));

    // 사업자 정보 조회
    BusinessProfile businessProfile = businessProfileRepository.findByTargetIdAndTargetType(advertisementId,
                    TargetType.ADVERTISEMENT)
            .orElseThrow(() -> new CustomException(CustomErrorCode.BUSINESS_NOT_EXIST));

    // 광고 결제 정보 조회
    AdPaymentInfo adPaymentInfo = adPaymentInfoRepository.findByAdvertisementId(advertisementId)
            .orElseThrow(() -> new CustomException(CustomErrorCode.PAYMENT_INFO_NOT_FOUND));

    return advertisementRefundReceiptMapper.toRefundReceiptDto(advertisement, businessProfile, adPaymentInfo);
  }

  @Override
  public AdRejectInfoResponse getAdvertisementRejectInfo(Long memberId, Long advertisementId) {
    // 광고가 해당 회원의 것인지 확인
    Advertisement advertisement = adRepository.findByIdAndMemberId(advertisementId, memberId)
            .orElseThrow(() -> new CustomException(CustomErrorCode.AD_NOT_FOUND));

    // 거절 정보 조회
    RejectInfo rejectInfo = rejectInfoRepository.findByTargetIdAndTargetType(advertisementId, TargetType.ADVERTISEMENT)
            .orElseThrow(() -> new CustomException(CustomErrorCode.REJECT_INFO_NOT_FOUND));

    return AdRejectInfoResponse.builder()
            .description(rejectInfo.getDescription())
            .rejectedAt(rejectInfo.getCreatedAt())
            .build();
  }

  @Override
  @Transactional
  public void completeAdvertisementPayment(Long memberId, Long advertisementId) {

    // 광고 조회 및 권한 확인
    Advertisement advertisement = adRepository.findByIdAndMemberId(advertisementId, memberId)
            .orElseThrow(() -> new CustomException(CustomErrorCode.AD_NOT_FOUND));

    AdvertisementStatus oldStatus = advertisement.getStatus();

    // 1. 광고 상태를 PENDING_PUBLISH로 변경
    advertisement.updateStatus(AdvertisementStatus.PENDING_PUBLISH);
    adRepository.save(advertisement);

    // 2. AdPaymentInfo 상태를 PENDING에서 SUCCESS로 업데이트
    AdPaymentInfo paymentInfo = adPaymentInfoRepository.findByAdvertisementId(advertisementId)
            .orElseThrow(() -> new CustomException(CustomErrorCode.PAYMENT_INFO_NOT_FOUND));
    paymentInfo.updateStatus( PaymentStatus.SUCCESS);
    adPaymentInfoRepository.save(paymentInfo);

    AdvertisementStatus newStatus = advertisement.getStatus();

    notificationService.notifyAdStatusChange(
            advertisement,
            oldStatus,
            newStatus
    );

    adSystemService.updateAdStatus();
    log.info("광고 결제 완료 처리 - 광고 ID: {}, 회원 ID: {}", advertisementId, memberId);
  }

  @Override
  public AdRefundReceiptResponse getAdvertisementRefundHistory(Long memberId, Long advertisementId) {
    // 광고가 해당 회원의 것인지 확인
    Advertisement advertisement = adRepository.findByIdAndMemberId(advertisementId, memberId)
            .orElseThrow(() -> new CustomException(CustomErrorCode.AD_NOT_FOUND));

    // 2) 비즈니스 프로필 조회 (영수증 표시용)
    BusinessProfile businessProfile = businessProfileRepository
            .findByTargetIdAndTargetType(advertisementId, TargetType.ADVERTISEMENT)
            .orElseThrow(() -> new CustomException(CustomErrorCode.BUSINESS_NOT_EXIST));

    // 광고 결제 정보 조회
    AdPaymentInfo adPaymentInfo = adPaymentInfoRepository.findByAdvertisementId(advertisementId)
            .orElseThrow(() -> new CustomException(CustomErrorCode.PAYMENT_INFO_NOT_FOUND));

    RefundInternalResponse refund = refundInternalService.getRefundByTarget(
            PaymentTargetType.AD, advertisementId);

    return adRefundReceiptMapper.toRefundHistoryDto(advertisement, businessProfile, adPaymentInfo, refund);
  }
}
