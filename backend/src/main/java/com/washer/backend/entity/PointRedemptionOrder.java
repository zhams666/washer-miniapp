package com.washer.backend.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("point_redemption_order")
public class PointRedemptionOrder {

    @TableId
    private Long id;
    private String redemptionNo;
    private String requestNo;
    private Long userId;
    private Long productId;
    private String productTitleSnapshot;
    private Integer pointsAmount;
    private String fulfillmentStatus;
    private String fulfillmentReference;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
