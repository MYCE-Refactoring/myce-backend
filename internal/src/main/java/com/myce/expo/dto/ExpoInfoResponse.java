package com.myce.expo.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ExpoInfoResponse {
    private String title;
    private String location;
    private String locationDetail;
    private Long ownerMemberId;
    private List<TicketInfo> ticketInfos;
}
