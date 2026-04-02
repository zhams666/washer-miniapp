package com.washer.backend.dto.admin;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AdminWalletTransactionCenterItem {

    private Long id;
    private String transactionNo;
    private Long userId;
    private Long storeId;
    private String storeName;
    private String bizType;
    private String amountType;
    private String changeType;
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private Long relatedOrderId;
    private String relatedOrderNo;
    private String remark;
    private LocalDateTime createdAt;
}
