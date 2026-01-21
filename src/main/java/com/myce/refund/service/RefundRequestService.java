package com.myce.refund.service;

import com.myce.refund.dto.RefundRequestDto;


/*
  - 기존: PaymentRepository/RefundRepository 직접 조회 + Refund 저장
  - 변경: RefundInternalService로 요청 → payment에서 PENDING 생성/검증
  - core는 상태 변경/알림/마일리지만 처리
 */
public interface RefundRequestService {
    
    // 박람회 환불 신청
    void createRefundRequest(Long memberId, Long expoId, RefundRequestDto requestDto);
    
    // 개인 예매 환불 (즉시 처리)
    void createReservationRefund(Long memberId, Long reservationId, String reason);
    
}