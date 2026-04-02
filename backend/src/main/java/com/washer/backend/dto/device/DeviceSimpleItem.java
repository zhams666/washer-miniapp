package com.washer.backend.dto.device;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DeviceSimpleItem {

    private Long id;
    private String deviceCode;
    private String deviceName;
    private Long storeId;
    private String storeName;
    private String deviceType;
    private String deviceRole;
    private String status;
    private String deviceStatus;
    private String protocolType;
    private String firmwareVersion;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
