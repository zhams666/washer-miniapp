package com.washer.backend.dto.admin;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardDeviceAlertItem {

    private Long deviceId;
    private String deviceCode;
    private String deviceName;
    private Long storeId;
    private String storeName;
    private String status;
    private LocalDateTime lastHeartbeatTime;
    private LocalDateTime lastOnlineTime;
    private LocalDateTime updatedAt;
    private String remark;
}
