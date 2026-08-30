package com.washer.backend.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("wash_queue")
public class WashQueue {

    @TableId
    private Long id;

    private String queueNo;
    private Long userId;
    private Long storeId;
    private String queueStatus;
    private BigDecimal userLatitude;
    private BigDecimal userLongitude;
    private BigDecimal distanceKm;
    private String cancelReason;
    private LocalDateTime queuedAt;
    private LocalDateTime cancelledAt;
    private LocalDateTime updatedAt;
}
