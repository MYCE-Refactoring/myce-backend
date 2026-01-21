package com.myce.system.repository.querydsl;

import com.myce.system.entity.AdFeeSetting;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdFeeSettingRepositoryCustom {

    Page<AdFeeSetting> search(Long positionId, String name, Pageable pageable);
}

