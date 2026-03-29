package com.washer.backend.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("wash_order_payment_detail")
public class WashOrderPaymentDetail {

    @TableId
    private Long id;

    private Long orderId;
    private String orderNo;
    private Long userId;
    private Long consumeStoreId;
    private String sourceType;
    private Long sourceStoreId;
    private String amountType;
    private Long userCardId;
    private BigDecimal amount;
    private Integer deductTimes;
    private Integer paymentSeq;
    private String settleStage;
    private String allocationStrategy;
    private String bizActionNo;
    private BigDecimal refundedAmount;
    private LocalDateTime createdAt;
}
