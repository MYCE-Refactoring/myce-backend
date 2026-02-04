package com.myce.reservation.repository.impl;



import com.myce.reservation.dto.RecipientInfoDto;

import java.util.List;

public interface ReserverRepositoryCustom {

    List<RecipientInfoDto> searchByFilter(Long expoId, String entranceStatus, String name,
                                          String phone, String reservationCode, String ticketName);
}

