package com.washer.backend.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("user_vehicle")
public class UserVehicle {

    @TableId
    private Long id;

    private Long userId;
    private String plateNo;
    private String vehicleBrand;
    private String vehicleModel;
    private String vehicleColor;
    private Integer isDefault;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
