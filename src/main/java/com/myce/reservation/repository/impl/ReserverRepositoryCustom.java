package com.myce.reservation.repository.impl;

import com.myce.notification.internal.dto.RecipientInfoDto;
import com.myce.system.entity.AdFeeSetting;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReserverRepositoryCustom {

    List<RecipientInfoDto> searchByFilter(Long expoId, String entranceStatus, String name,
                                          String phone, String reservationCode, String ticketName);
}

