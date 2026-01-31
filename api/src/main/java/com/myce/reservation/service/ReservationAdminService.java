package com.myce.reservation.service;

import com.myce.auth.dto.type.LoginType;
import com.myce.reservation.dto.ExpoAdminReservationResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ReservationAdminService {
    List<String>  getExpoTicketNames(Long expoId,
                                     Long memberId,
                                     LoginType loginType);
    Page<ExpoAdminReservationResponse> getMyExpoReservations(Long expoId,
                                                             Long memberId,
                                                             LoginType loginType,
                                                             String entranceStatus,
                                                             String name,
                                                             String phone,
                                                             String reservationCode,
                                                             String ticketName,
                                                             Pageable pageable);
}