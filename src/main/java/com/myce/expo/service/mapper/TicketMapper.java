package com.myce.expo.service.mapper;

import com.myce.expo.dto.TicketSummaryResponse;
import com.myce.expo.entity.Ticket;

import java.util.List;

public class TicketMapper {

    public static List<TicketSummaryResponse> toSummaryResponses(List<Ticket> tickets) {
        return tickets.stream()
                .map(t -> TicketSummaryResponse.builder()
                        .ticketId(t.getId())
                        .name(t.getName())
                        .type(t.getType())
                        .price(t.getPrice())
                        .remainingQuantity(t.getRemainingQuantity())
                        .saleStartDate(t.getSaleStartDate())
                        .saleEndDate(t.getSaleEndDate())
                        .description(t.getDescription())
                        .build())
                .toList();
    }

}
