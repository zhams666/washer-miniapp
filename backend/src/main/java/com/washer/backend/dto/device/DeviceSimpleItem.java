package com.washer.backend.dto.device;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Data;

@Data
@NoArgsConstructor
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
    private String agentLevel;
    private String contactName;
    private String contactPhone;
    private Integer baseTimeMinutes;
    private BigDecimal basePrice;
    private BigDecimal overtimePrice;
    private String speakerSn;
    private String speakerVersion;
    private String cabinetName;
    private String doorState;
    private String powerState;
    private Boolean maintenanceMode;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
