package com.washer.backend.dto.admin;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class AdminDashboardDeviceStatus {

    private long totalCount;
    private long normalCount;
    private long runningCount;
    private long idleCount;
    private long faultCount;
    private long offlineCount;
    private long disabledCount;
    private long pausedCount;
    private long abnormalCount;
    private List<AdminDashboardDeviceAlertItem> alerts = new ArrayList<>();
}
