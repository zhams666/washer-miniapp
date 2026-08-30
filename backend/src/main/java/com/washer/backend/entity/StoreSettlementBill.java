package com.washer.backend.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("store_settlement_bill")
public class StoreSettlementBill {

    @TableId
    private Long id;

    private String billNo;
    private Long fromStoreId;
    private Long toStoreId;
    private String settlementPeriodType;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer totalOrderCount;
    private BigDecimal totalAmount;
    private BigDecimal totalRefundAmount;
    private BigDecimal netAmount;
    private String settlementStatus;
    private String lockStatus;
    private LocalDateTime lockedAt;
    private LocalDateTime confirmedAt;
    private LocalDateTime paidAt;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
