package com.washer.backend.dto.miniadmin;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class MiniAdminDeviceStatusSummary {

    private long totalCount;
    private long runningCount;
    private long idleCount;
    private long faultCount;
    private long offlineCount;
    private long disabledCount;
    private long pausedCount;
    private long abnormalCount;
    private List<MiniAdminDeviceAlertItem> alerts = new ArrayList<>();
}
