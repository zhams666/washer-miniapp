package com.washer.backend.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("card_purchase_order")
public class CardPurchaseOrder {

    @TableId
    private Long id;

    private String purchaseOrderNo;
    private Long userId;
    private Long storeId;
    private Long cardProductId;
    private String cardType;
    private String sourceChannel;
    private Integer buyCount;
    private BigDecimal payAmount;
    private String payStatus;
    private LocalDateTime purchaseTime;
    private String externalOrderNo;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
