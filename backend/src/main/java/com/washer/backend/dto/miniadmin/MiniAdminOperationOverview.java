package com.washer.backend.dto.miniadmin;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MiniAdminOperationOverview {

    private String tierCode;
    private String tierName;
    private String headline;
    private String description;
    private String scopeName;
    private LocalDate bizDate;
    private LocalDateTime generatedAt;
    private MiniAdminStoreOption activeStore;
    private List<MiniAdminStoreOption> stores;
    private List<MiniAdminScopeSummaryItem> scopeSummary;
    private List<MiniAdminMetricItem> metrics;
    private MiniAdminDeviceStatusSummary deviceStatus;
    private List<MiniAdminStoreRankingItem> storeRankings;
    private List<MiniAdminRecentActivityItem> recentActivities;
}
