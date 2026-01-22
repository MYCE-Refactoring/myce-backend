package com.myce.payment.component;

import com.myce.common.exception.CustomErrorCode;
import com.myce.common.exception.CustomException;
import com.myce.reservation.dto.PreReservationCacheDto;
import com.myce.reservation.repository.PreReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.stereotype.Component;

/**
 * 결제 데이터 준비 전담 컴포넌트
 * - Redis 조회/삭제 담당
 * - 컨트롤러와 서비스 사이의 데이터 준비 계층
 */

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentDataPreparationComponent {
    private final PreReservationRepository preReservationRepository;

    /**
     * Redis에서 PreReservation 조회
     */
    public PreReservationCacheDto prepareReservationData(String sessionId) {
        return getFromRedis(sessionId);
    }

    /**
     * Redis 세션 정리
     */
    public void cleanupSession(String sessionId) {
        try {
            preReservationRepository.deleteBySessionId(sessionId);
            log.info("Redis 세션 정리 완료 - sessionId: {}", sessionId);
        } catch (RedisConnectionFailureException e) {
            log.warn("Redis 연결 실패로 세션 정리 실패 - sessionId: {}", sessionId, e);
        } catch (Exception e) {
            log.warn("Redis 세션 정리 중 예상치 못한 오류 - sessionId: {}", sessionId, e);
        }
    }


    private PreReservationCacheDto getFromRedis(String sessionId) {
        // 1. sessionId 검증
        if (sessionId == null || sessionId.isBlank()) {
            throw new CustomException(CustomErrorCode.PAYMENT_SESSION_EXPIRED);
        }

        try {
            // 2. Redis 조회
            PreReservationCacheDto cacheDto =
                    preReservationRepository.findBySessionId(sessionId);

            // 3. null 체크
            if (cacheDto == null) {
                log.error("결제 세션 만료 - sessionId: {}", sessionId);
                throw new CustomException(CustomErrorCode.PAYMENT_SESSION_EXPIRED);
            }

            log.info("Redis 세션 조회 성공 - sessionId: {}", sessionId);
            return cacheDto;

        } catch (RedisConnectionFailureException e) {
            log.error("Redis 연결 실패", e);
            throw new CustomException(CustomErrorCode.INTERNAL_SERVER_ERROR);
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.error("Redis 조회 실패", e);
            throw new CustomException(CustomErrorCode.INTERNAL_SERVER_ERROR);
        }
    }


}