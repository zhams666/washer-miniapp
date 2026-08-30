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
public class MiniAdminDashboardOverview {

    private LocalDate bizDate;
    private LocalDateTime generatedAt;
    private MiniAdminStoreOption activeStore;
    private List<MiniAdminStoreOption> stores;
    private List<MiniAdminMetricItem> metrics;
    private MiniAdminDeviceStatusSummary deviceStatus;
    private List<MiniAdminRecentActivityItem> recentActivities;
}
