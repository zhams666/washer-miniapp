package com.washer.backend.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("wallet_transaction")
public class WalletTransaction {

    @TableId
    private Long id;

    private String transactionNo;
    private Long userId;
    private Long storeId;
    private String bizType;
    private String amountType;
    private String balanceBucket;
    private String changeType;
    private String freezeType;
    private BigDecimal amount;
    private String relatedAction;
    private String bizActionNo;
    private BigDecimal balanceBefore;
    private BigDecimal balanceAfter;
    private Long relatedOrderId;
    private String relatedOrderNo;
    private String remark;
    private LocalDateTime createdAt;
}
