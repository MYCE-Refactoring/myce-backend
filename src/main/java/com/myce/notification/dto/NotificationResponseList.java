package com.myce.notification.dto;


import com.myce.system.dto.message.MessageSummaryResponse;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class NotificationResponseList {
    int currentPage;
    int totalPage;
    List<NotificationResponse> notifications;

    public NotificationResponseList(int currentPage, int totalPage) {
        this.currentPage = currentPage;
        this.totalPage = totalPage;
        this.notifications = new ArrayList<>();
    }
}
