package com.myce.notification.dto;

import com.myce.notification.document.type.NotificationType;
import com.myce.notification.document.type.NotificationTargetType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class NotificationResponse {
    private String notificationId;
    private NotificationType type;
    private NotificationTargetType targetType;
    private Long targetId;
    private String title;
    private String content;
    private Boolean isRead;
    private LocalDateTime createdAt;
}