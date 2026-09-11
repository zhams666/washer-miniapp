package com.washer.backend.dto.miniadmin;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class MiniAdminDeviceConfigRequest {

    private String deviceCode;
    private String deviceName;
    private Integer baseTimeMinutes;
    private BigDecimal basePrice;
    private BigDecimal overtimePrice;
    private String speakerSn;
    private String speakerVersion;
    private String cabinetName;
}
