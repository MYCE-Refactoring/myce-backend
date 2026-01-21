package com.myce.qrcode.dashboard.service.platform;

import com.myce.qrcode.dashboard.dto.platform.UsageDashboardResponse;
import com.myce.qrcode.dashboard.dto.platform.type.PeriodType;

public interface UsageService {
    UsageDashboardResponse getUsageDashboard(PeriodType period, long chartSize);
}
