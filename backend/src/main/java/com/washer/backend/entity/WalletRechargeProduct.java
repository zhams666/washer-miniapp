package com.washer.backend.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("wallet_recharge_product")
public class WalletRechargeProduct {

    @TableId
    private Long id;

    private Long storeId;
    private String productName;
    private BigDecimal payAmount;
    private BigDecimal principalAmount;
    private BigDecimal giftAmount;
    private LocalDateTime effectiveTime;
    private LocalDateTime expireTime;
    private Integer purchaseLimit;
    private Integer status;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
