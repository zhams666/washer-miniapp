package com.washer.backend.dto.admin;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class AdminSettlementBillItem {

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
    private LocalDateTime createdAt;
}
