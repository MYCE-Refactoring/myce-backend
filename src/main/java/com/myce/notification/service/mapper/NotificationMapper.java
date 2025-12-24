package com.myce.notification.service.mapper;

import com.myce.notification.document.Notification;
import com.myce.notification.document.type.NotificationTargetType;
import com.myce.notification.document.type.NotificationType;
import com.myce.notification.dto.NotificationResponse;
import com.myce.notification.dto.NotificationResponseList;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

@Component
public class NotificationMapper {


    public Notification toEntity(
            Long memberId,
            Long targetId,
            NotificationType type,
            NotificationTargetType targetType,
            String title,
            String content
    ){
        return Notification.builder()
                .memberId(memberId)
                .type(type)
                .targetType(targetType)
                .targetId(targetId)
                .title(title)
                .content(content)
                .isRead(false)
                .build();
    }

    public NotificationResponse toResponse(Notification notification) {
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

    public NotificationResponseList toResponseList(Page<Notification> pages) {
        NotificationResponseList responseList =
                new NotificationResponseList(pages.getNumber()+1, pages.getTotalPages());

        for (Notification notification : pages.getContent()) {
            responseList.getNotifications().add(toResponse(notification));
        }


        return responseList;
    }
}
