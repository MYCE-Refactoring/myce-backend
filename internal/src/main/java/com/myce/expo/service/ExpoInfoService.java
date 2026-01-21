package com.myce.expo.service;

import com.myce.common.exception.CustomErrorCode;
import com.myce.common.exception.CustomException;
import com.myce.expo.dto.AdminCodeInfo;
import com.myce.expo.dto.ExpoInfoListResponse;
import com.myce.expo.dto.ExpoInfoResponse;
import com.myce.expo.dto.TicketInfo;
import com.myce.expo.entity.AdminCode;
import com.myce.expo.entity.AdminPermission;
import com.myce.expo.entity.Expo;
import com.myce.expo.entity.Ticket;
import com.myce.expo.repository.AdminCodeRepository;
import com.myce.expo.repository.ExpoRepository;
import com.myce.expo.repository.TicketRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExpoInfoService {

    private final ExpoRepository expoRepository;
    private final TicketRepository ticketRepository;
    private final AdminCodeRepository adminCodeRepository;

    public ExpoInfoListResponse getRecentExpoInfos(int count) {
        if (count <= 0) {
            throw new CustomException(CustomErrorCode.EXPO_NOT_FOUND);
        }

        List<Expo> expoList = expoRepository.findRecentExpo(count);

        ExpoInfoListResponse response = new ExpoInfoListResponse();
        for (Expo expo : expoList) {
            List<TicketInfo> ticketInfos = getTicketInfos(expo.getId());
            ExpoInfoResponse expoInfo = toResponse(expo, ticketInfos);
            response.addExpoInfo(expoInfo);
        }

        return response;
    }

    public ExpoInfoResponse getExpoInfo(Long expoId) {
        Expo expo = expoRepository.findById(expoId)
                .orElseThrow(() -> new CustomException(CustomErrorCode.EXPO_NOT_EXIST));

        List<TicketInfo> ticketInfos = getTicketInfos(expo.getId());

        return toResponse(expo, ticketInfos);
    }

    public AdminCodeInfo getAdminCodeInfo(Long adminId) {
        AdminCode adminCode = adminCodeRepository.findById(adminId)
                .orElseThrow(() -> new CustomException(CustomErrorCode.ADMIN_CODE_NOT_FOUND));
        AdminPermission adminPermission = adminCode.getAdminPermission();

        return new AdminCodeInfo(
                adminCode.getCode(),
                adminCode.getExpoId(),
                adminPermission.getIsInquiryView());
    }

    public boolean isAdminAccessToExpo(Long expoId, Long adminId) {
        if (expoId == null || adminId == null) return false;

        Optional<AdminCode> adminCodeOptional = adminCodeRepository.findById(adminId);
        if (adminCodeOptional.isEmpty()) return false;

        AdminCode adminCode = adminCodeOptional.get();
        return expoId.equals(adminCode.getExpoId()) && adminCode.getAdminPermission().getIsInquiryView();
    }

    public boolean isMemberOwnerToExpo(Long expoId, Long memberId) {
        if (expoId == null || memberId == null) return false;

        Optional<Long> ownerIdOptional = expoRepository.findMemberIdById(expoId);
        if (ownerIdOptional.isEmpty()) return false;

        log.debug("Find expo owner for check same memberId. expoId={}, memberId={}", expoId, memberId);
        return memberId.equals(ownerIdOptional.get());
    }

    private List<TicketInfo> getTicketInfos(Long expoId) {
        List<Ticket> tickets = ticketRepository.findByExpoId(expoId);

        List<TicketInfo> ticketInfos = new ArrayList<>();
        for (Ticket ticket: tickets) {
            TicketInfo ticketInfo = new TicketInfo(
                    ticket.getName(),
                    ticket.getPrice(),
                    ticket.getSaleStartDate(),
                    ticket.getSaleEndDate(),
                    ticket.getTotalQuantity(),
                    ticket.getRemainingQuantity()
            );
            ticketInfos.add(ticketInfo);
        }

        return ticketInfos;
    }

    private ExpoInfoResponse toResponse(Expo expo, List<TicketInfo> ticketInfos) {
        return new ExpoInfoResponse(
                expo.getTitle(),
                expo.getLocation(),
                expo.getLocationDetail(),
                expo.getMember().getId(),
                ticketInfos
        );
    }
}
