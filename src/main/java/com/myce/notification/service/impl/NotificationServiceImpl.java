package com.myce.notification.service.impl;

import com.myce.advertisement.entity.Advertisement;
import com.myce.advertisement.entity.type.AdvertisementStatus;
import com.myce.expo.entity.Expo;
import com.myce.expo.entity.type.ExpoStatus;
import com.myce.notification.document.Notification;
import com.myce.notification.dto.ExpoStatusChangeCommand;
import com.myce.notification.dto.AdStatusChangeCommand;
import com.myce.notification.dto.NotificationResponse;
import com.myce.notification.entity.type.NotificationType;
import com.myce.notification.entity.type.NotificationTargetType;
import com.myce.notification.repository.NotificationRepository;
import com.myce.notification.service.NotificationService;
import com.myce.notification.service.SseService;
import com.myce.reservation.entity.Reservation;
import com.myce.reservation.entity.code.UserType;
import com.myce.system.entity.MessageTemplateSetting;
import com.myce.system.entity.type.ChannelType;
import com.myce.system.entity.type.MessageTemplateCode;
import com.myce.system.repository.MessageTemplateSettingRepository;
import com.myce.common.exception.CustomException;
import com.myce.common.exception.CustomErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

import static com.myce.system.entity.type.MessageTemplateCode.QR_ISSUED;
import static com.myce.system.entity.type.MessageTemplateCode.QR_REISSUED;
import static com.myce.system.entity.type.MessageTemplateCode.PAYMENT_COMPLETE;
import static com.myce.system.entity.type.MessageTemplateCode.EVENT_REMINDER;

import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final SseService sseService;
    private final MessageTemplateSettingRepository messageTemplateSettingRepository;

    @Override
    public void saveNotification(Long memberId, Long targetId, String title, String content, 
                                NotificationType type, NotificationTargetType targetType) {
        try {
            Notification notification = Notification.builder()
                    .memberId(memberId)
                    .type(type)
                    .targetType(targetType)
                    .targetId(targetId)
                    .title(title)
                    .content(content)
                    .isRead(false)
                    .build();

            notificationRepository.save(notification);
            
            // SSE 실시간 알림 전송
            String message = String.format(
                "{\"type\":\"%s\",\"message\":\"%s\"}",
                type.name(),
                content
            );
            sseService.notifyMemberViaSseEmitters(memberId, message);
            
            log.info("알림 저장 및 SSE 전송 완료 - 회원 ID: {}, 제목: {}, 타입: {}", memberId, title, type);
        } catch (Exception e) {
            log.error("알림 저장 실패 - 회원 ID: {}, 타입: {}, 오류: {}", memberId, type, e.getMessage(), e);
        }
    }

    @Override
    public Page<NotificationResponse> getNotificationsByMemberId(
            Long memberId,
            Pageable pageable
    ) {
        // 알림은 서버에서 정렬 고정 (최신순)
        Pageable fixedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<Notification> page =
                notificationRepository.findByMemberId(memberId, fixedPageable);

        return page.map(this::convertToResponse);
    }


    @Override
    public void markAsRead(String notificationId, Long memberId) {
        try {
            // MongoDB @Update를 사용하여 원자적 업데이트 수행
            // Query 조건에 memberId도 포함하여 권한 체크와 업데이트를 한번에 처리
            notificationRepository.markAsRead(notificationId, memberId, LocalDateTime.now());
            log.info("알림 읽음 처리 완료 - 알림 ID: {}, 회원 ID: {}", notificationId, memberId);
        } catch (Exception e) {
            log.error("알림 읽음 처리 실패 - 알림 ID: {}, 회원 ID: {}, 오류: {}", notificationId, memberId, e.getMessage(), e);
        }
    }

    @Override
    public void markAllAsRead(Long memberId) {
        try {
            // 해당 회원의 모든 읽지 않은 알림을 읽음 처리
            notificationRepository.markAllAsReadByMemberId(memberId, LocalDateTime.now());
            log.info("모든 알림 읽음 처리 완료 - 회원 ID: {}", memberId);
        } catch (Exception e) {
            log.error("모든 알림 읽음 처리 실패 - 회원 ID: {}, 오류: {}", memberId, e.getMessage(), e);
        }
    }

    @Override
    public void sendQrIssuedNotification(Long memberId, Long reservationId, String expoTitle, boolean isReissue) {
        try {

            MessageTemplateCode templateCode = isReissue ? QR_REISSUED : QR_ISSUED;
            MessageTemplateSetting template = messageTemplateSettingRepository
                    .findByCodeAndChannelType(templateCode, ChannelType.NOTIFICATION)
                    .orElseThrow(() -> new CustomException(CustomErrorCode.NOT_EXIST_MESSAGE_TEMPLATE));

            String title = template.getSubject();
            String content = template.getContent().replace("{expoTitle}", expoTitle);
            
            // 공통 saveNotification 메서드 호출
            saveNotification(memberId, reservationId, title, content, 
                           NotificationType.QR_ISSUED, NotificationTargetType.RESERVATION);
                           
            log.info("QR 발급 알림 처리 완료 - 회원 ID: {}, 예매 ID: {}", memberId, reservationId);
        } catch (Exception e) {
            log.error("QR 발급 알림 처리 실패 - 회원 ID: {}, 예매 ID: {}, 오류: {}", memberId, reservationId, e.getMessage(), e);
        }
    }

    @Override
    public void sendPaymentCompleteNotification(Long memberId, Long reservationId, String expoTitle, String paymentAmount) {
        try {
            MessageTemplateSetting template = messageTemplateSettingRepository
                    .findByCodeAndChannelType(PAYMENT_COMPLETE, ChannelType.NOTIFICATION)
                    .orElseThrow(() -> new CustomException(CustomErrorCode.NOT_EXIST_MESSAGE_TEMPLATE));

            String title = template.getSubject();
            String content = template.getContent()
                    .replace("{expoTitle}", expoTitle)
                    .replace("{paymentAmount}", paymentAmount);
            
            // 공통 saveNotification 메서드 호출
            saveNotification(memberId, reservationId, title, content, 
                           NotificationType.RESERVATION_CONFIRM, NotificationTargetType.RESERVATION);

                           
            log.info("결제 완료 알림 처리 완료 - 회원 ID: {}, 예매 ID: {}, 금액: {}", memberId, reservationId, paymentAmount);
        } catch (Exception e) {
            log.error("결제 완료 알림 처리 실패 - 회원 ID: {}, 예매 ID: {}, 오류: {}", memberId, reservationId, e.getMessage(), e);
        }
    }

    @Override
    public void sendExpoStartNotification(List<Long> userIds, String expoTitle, Long expoId) {
        try {
            // 메시지 템플릿 조회
            MessageTemplateSetting template = messageTemplateSettingRepository.findByCodeAndChannelType(
                            MessageTemplateCode.EXPO_REMINDER, ChannelType.NOTIFICATION)
                    .orElseThrow(() -> new CustomException(CustomErrorCode.NOT_EXIST_MESSAGE_TEMPLATE));

            
            if (userIds.isEmpty()) {
                log.info("알림 전송 대상이 없습니다 - 박람회 ID: {}", expoId);
                return;
            }

            String content = template.getContent().replace("{expoTitle}", expoTitle);

            // 각 예약자에게 알림 전송
            for (Long userId : userIds) {
                saveNotification(
                    userId, 
                    expoId, 
                    template.getSubject(), 
                    content,
                    NotificationType.EXPO_REMINDER,
                    NotificationTargetType.EXPO
                );
            }
            int notificationCount = userIds.size();
            
            log.info("박람회 시작 알림 처리 완료 - 박람회 ID: {}, 알림 수: {} 개", 
                    expoId, notificationCount);
                    
        } catch (Exception e) {
            log.error("박람회 시작 알림 전송 실패 - 박람회 ID: {}, 오류: {}", 
                    expoId, e.getMessage(), e);
        }
    }

    @Override
    public void sendEventHourReminderNotification(List<Long> memberIds, Long expoId, String expoTitle, String eventName, String startTime) {
        try {
            // 메시지 템플릿 조회
            MessageTemplateSetting template = messageTemplateSettingRepository.findByCodeAndChannelType(
                            EVENT_REMINDER, ChannelType.NOTIFICATION)
                    .orElseThrow(() -> new CustomException(CustomErrorCode.NOT_EXIST_MESSAGE_TEMPLATE));

            
            if (memberIds.isEmpty()) {
                log.info("1시간 전 알림 전송 대상이 없습니다 - 박람회 ID: {}", expoId);
                return;
            }

            String content = template.getContent()
                    .replace("{expoTitle}", expoTitle)
                    .replace("{eventName}", eventName)
                    .replace("{startTime}", startTime);

            // 각 예약자에게 알림 전송
            for (Long userId : memberIds) {
                saveNotification(
                    userId, 
                    expoId, 
                    template.getSubject(), 
                    content,
                    NotificationType.EVENT_REMINDER,
                    NotificationTargetType.EXPO
                );
            }
            int notificationCount = memberIds.size();
            
            log.info("행사 1시간 전 알림 처리 완료 - 박람회 ID: {}, 알림 수: {} 개", 
                    expoId, notificationCount);
                    
        } catch (Exception e) {
            log.error("행사 1시간 전 알림 전송 실패 - 박람회 ID: {}, 오류: {}", 
                    expoId, e.getMessage(), e);
        }
    }

    @Override
    public void sendExpoStatusChangeNotification(ExpoStatusChangeCommand command) {
        try {

            MessageTemplateSetting template = messageTemplateSettingRepository.findByCodeAndChannelType(
                            MessageTemplateCode.EXPO_STATUS_CHANGE, ChannelType.NOTIFICATION)
                    .orElseThrow(() -> new CustomException(CustomErrorCode.NOT_EXIST_MESSAGE_TEMPLATE));

            Long memberId = command.getMemberId();
            Long expoId = command.getExpoId();
            String expoTitle = command.getExpoTitle();
            ExpoStatus oldStatus = command.getOldStatus();
            ExpoStatus newStatus = command.getNewStatus();


            String title = template.getSubject();
            String content = template.getContent()
                    .replace("{expoTitle}", expoTitle)
                    .replace("{oldStatus}", getStatusDisplayName(oldStatus))
                    .replace("{newStatus}", getStatusDisplayName(newStatus));

            saveNotification(memberId, expoId, title, content,
                    NotificationType.EXPO_STATUS_CHANGE, NotificationTargetType.EXPO_STATUS);


            log.info("박람회 상태 변경 알림 처리 완료 - 박람회 ID: {}, 회원 ID: {}, {} -> {}",
                    expoId, memberId, oldStatus, newStatus);

        } catch (Exception e) {
            log.error("박람회 상태 변경 알림 전송 실패 - 오류: {}", e.getMessage(), e);
        }
    }

    @Override
    public void sendAdvertisementStatusChangeNotification(AdStatusChangeCommand command) {
        try {
            MessageTemplateSetting template =
                    messageTemplateSettingRepository.findByCodeAndChannelType(
                            MessageTemplateCode.AD_STATUS_CHANGE,
                            ChannelType.NOTIFICATION
                    ).orElseThrow(() -> new CustomException(CustomErrorCode.NOT_EXIST_MESSAGE_TEMPLATE));

            String content = template.getContent()
                    .replace("{adTitle}", command.getAdTitle())
                    .replace("{oldStatus}", getAdStatusDisplayName(command.getOldStatus()))
                    .replace("{newStatus}", getAdStatusDisplayName(command.getNewStatus()));

            saveNotification(
                    command.getMemberId(),
                    command.getAdId(),
                    template.getSubject(),
                    content,
                    NotificationType.AD_STATUS_CHANGE,
                    NotificationTargetType.AD_STATUS
            );

        } catch (Exception e) {
            log.error("광고 상태 변경 알림 전송 실패", e);
        }
    }


    private String getStatusDisplayName(ExpoStatus status) {
        switch (status) {
            case PENDING_APPROVAL:
                return "승인 대기";
            case PENDING_PAYMENT:
                return "결제 대기";
            case PENDING_PUBLISH:
                return "게시 대기";
            case PENDING_CANCEL:
                return "취소 대기";
            case PUBLISHED:
                return "게시 중";
            case PUBLISH_ENDED:
                return "게시 종료";
            case SETTLEMENT_REQUESTED:
                return "정산 요청";
            case COMPLETED:
                return "종료됨";
            case REJECTED:
                return "승인 거절";
            case CANCELLED:
                return "취소 완료";
            default:
                return status.name();
        }
    }

    private String getAdStatusDisplayName(AdvertisementStatus status) {
        switch (status) {
            case PENDING_APPROVAL:
                return "승인 대기";
            case PENDING_PAYMENT:
                return "결제 대기";
            case PENDING_PUBLISH:
                return "게시 대기";
            case PENDING_CANCEL:
                return "취소 대기";
            case PUBLISHED:
                return "게시 중";
            case COMPLETED:
                return "종료됨";
            case REJECTED:
                return "승인 거절";
            case CANCELLED:
                return "취소 완료";
            default:
                return status.name();
        }
    }


    private NotificationResponse convertToResponse(Notification notification) {
        return NotificationResponse.builder()
                .notificationId(notification.getNotificationId())
                .type(notification.getType())
                .targetType(notification.getTargetType())
                .targetId(notification.getTargetId())
                .title(notification.getTitle())
                .content(notification.getContent())
                .isRead(notification.getIsRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}