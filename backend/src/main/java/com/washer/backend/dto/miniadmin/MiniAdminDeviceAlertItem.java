package com.washer.backend.dto.miniadmin;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MiniAdminDeviceAlertItem {

    private Long deviceId;
    private String deviceCode;
    private String deviceName;
    private Long storeId;
    private String storeName;
    private String status;
    private LocalDateTime lastHeartbeatTime;
    private LocalDateTime updatedAt;
}
