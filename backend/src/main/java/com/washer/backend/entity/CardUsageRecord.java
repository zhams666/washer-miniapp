package com.washer.backend.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("card_usage_record")
public class CardUsageRecord {

    @TableId
    private Long id;

    private String usageNo;
    private Long userCardId;
    private Long userId;
    private Long storeId;
    private Long orderId;
    private String orderNo;
    private Integer usedTimes;
    private LocalDateTime usageTime;
    private String operatorType;
    private Long operatorId;
    private String remark;
    private LocalDateTime createdAt;
}
