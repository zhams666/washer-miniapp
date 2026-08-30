package com.washer.backend.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("payment_transaction")
public class PaymentTransaction {

    @TableId
    private Long id;

    private String paymentNo;
    private String requestNo;
    private String bizType;
    private Long bizOrderId;
    private String bizOrderNo;
    private String bizActionNo;
    private Long userId;
    private Long storeId;
    private String payChannel;
    private String channelTradeNo;
    private String outTradeNo;
    private String prepayId;
    private String idempotencyKey;
    private String payStatus;
    private BigDecimal payAmount;
    private String currencyCode;
    private Integer callbackCount;
    private LocalDateTime requestTime;
    private LocalDateTime payTime;
    private LocalDateTime callbackTime;
    private LocalDateTime expireTime;
    private String remark;
    private String failReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
