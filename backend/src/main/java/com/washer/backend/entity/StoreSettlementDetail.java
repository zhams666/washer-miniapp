package com.washer.backend.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("store_settlement_detail")
public class StoreSettlementDetail {

    @TableId
    private Long id;

    private Long fromStoreId;
    private Long toStoreId;
    private Long userId;
    private Long orderId;
    private String orderNo;
    private Long paymentDetailId;
    private BigDecimal principalAmount;
    private BigDecimal refundAdjustAmount;
    private BigDecimal netAmount;
    private LocalDate bizDate;
    private String detailStatus;
    private Long billId;
    private String billNo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
