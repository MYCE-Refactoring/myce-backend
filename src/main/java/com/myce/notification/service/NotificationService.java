package com.myce.notification.service;

import com.myce.notification.dto.AdStatusChangeCommand;
import com.myce.notification.dto.ExpoStatusChangeCommand;
import com.myce.notification.dto.NotificationResponse;
import com.myce.notification.document.type.NotificationTargetType;
import com.myce.notification.document.type.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface NotificationService {
    void saveNotification(Long memberId, Long targetId, String title, String content,
                          NotificationType type, NotificationTargetType targetType);
    void sendQrIssuedNotification(Long memberId, Long reservationId, String expoTitle, boolean isReissue);
    void sendExpoStartNotification(List<Long> userIds, String expoTitle, Long expoId);
    void sendEventHourReminderNotification(List<Long> MemberIds, Long expoId, String expoTitle, String eventName, String startTime);
    void sendPaymentCompleteNotification(Long memberId, Long reservationId, String expoTitle, String paymentAmount);
    void sendExpoStatusChangeNotification(ExpoStatusChangeCommand command);
    void sendAdvertisementStatusChangeNotification(AdStatusChangeCommand command);
    Page<NotificationResponse> getNotificationsByMemberId(Long memberId, Pageable pageable);
    void markAsRead(String notificationId, Long memberId);
    void markAllAsRead(Long memberId);
}