package com.myce.qrcode.dashboard.service.platform;

import com.myce.qrcode.dashboard.dto.platform.RevenueDashboardResponse;
import com.myce.qrcode.dashboard.dto.platform.type.PeriodType;

public interface RevenueService {

    RevenueDashboardResponse getSettlementDashboard(PeriodType period, Long size);
}
