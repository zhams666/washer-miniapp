package com.washer.backend.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("recharge_order")
public class RechargeOrder {

    @TableId
    private Long id;

    private String rechargeOrderNo;
    private Long userId;
    private Long storeId;
    private Long rechargeProductId;
    private BigDecimal principalAmount;
    private BigDecimal giftAmount;
    private BigDecimal payAmount;
    private String payChannel;
    private String payStatus;
    private LocalDateTime payTime;
    private String thirdPartyTradeNo;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
