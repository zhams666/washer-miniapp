package com.washer.backend.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("membership_order")
public class MembershipOrder {

    @TableId
    private Long id;

    private String orderNo;
    private Long userId;
    private Long planId;
    private BigDecimal payAmount;
    private String payChannel;
    private String payStatus;
    private String paymentNo;
    private String thirdPartyTradeNo;
    private LocalDateTime payTime;
    private LocalDateTime memberStartTime;
    private LocalDateTime memberExpireTime;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
