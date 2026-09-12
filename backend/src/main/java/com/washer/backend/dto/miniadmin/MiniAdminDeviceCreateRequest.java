package com.washer.backend.dto.miniadmin;

import lombok.Data;

@Data
public class MiniAdminDeviceCreateRequest {

    private Long storeId;
    private String deviceCode;
    private String deviceName;
    private String deviceType;
    private String deviceRole;
    private String deviceStatus;
    private String protocolType;
    private String firmwareVersion;
    private String remark;
}
