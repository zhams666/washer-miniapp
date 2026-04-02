package com.washer.backend.dto.admin;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AdminPaymentDetailItem {

    private Long id;
    private Long orderId;
    private String orderNo;
    private Long userId;
    private Long storeId;
    private String storeName;
    private String payMode;
    private String paymentStatus;
    private String amountType;
    private Long userCardId;
    private String cardNo;
    private BigDecimal amount;
    private Integer deductTimes;
    private Integer paymentSeq;
    private String settleStage;
    private String allocationStrategy;
    private BigDecimal refundedAmount;
    private LocalDateTime createdAt;
}
