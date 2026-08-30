package com.washer.backend.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import lombok.Data;

@Data
@TableName("membership_setting")
public class MembershipSetting {

    @TableId
    private Long id;

    private String settingKey;
    private Integer memberDayEnabled;
    private Integer memberDayWeekday;
    private LocalTime memberDayStartTime;
    private LocalTime memberDayEndTime;
    private Integer memberDayFirstMinutes;
    private BigDecimal memberDayDiscountRate;
    private String benefitText;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
