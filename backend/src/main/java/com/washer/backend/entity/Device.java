package com.washer.backend.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("device")
public class Device {

    @TableId
    private Long id;

    private String deviceCode;
    private Long storeId;
    private Long bayId;
    private String deviceType;
    private String deviceRole;
    private String bayDeviceType;
    private String deviceName;
    private String deviceStatus;
    private String protocolType;
    private String firmwareVersion;
    private LocalDateTime lastHeartbeatTime;
    private LocalDateTime lastOnlineTime;
    private LocalDateTime installTime;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
