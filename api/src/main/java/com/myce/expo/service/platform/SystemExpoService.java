package com.myce.expo.service.platform;

import java.time.LocalDate;

public interface SystemExpoService {
    void checkAvailablePeriod(Long expoId, LocalDate startedAt, LocalDate endedAt);
    
    int publishPendingExpos();
    
    int closeCompletedExpos();
    
    void refreshExpoCache();
}