package com.washer.backend.dto.device;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DeviceSimpleItem {

    private Long id;
    private String deviceCode;
    private String deviceName;
    private Long storeId;
    private String status;
}
