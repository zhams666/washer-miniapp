package com.washer.backend.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("point_mall_product")
public class PointMallProduct {

    @TableId
    private Long id;

    private String title;
    private String description;
    private String coverImage;
    private String productType;
    private Integer pointsPrice;
    private Integer stockTotal;
    private Integer limitPerUser;
    private LocalDateTime effectiveTime;
    private LocalDateTime expireTime;
    private Integer status;
    private Integer sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
