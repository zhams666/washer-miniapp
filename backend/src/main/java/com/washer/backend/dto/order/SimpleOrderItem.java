package com.washer.backend.dto.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SimpleOrderItem {

    private Long id;
    private String orderNo;
    private Long userId;
    private Long storeId;
    private String storeName;
    private String orderStatus;
    private String paymentStatus;
    private BigDecimal estimatedAmount;
    private BigDecimal finalAmount;
    private LocalDateTime createdAt;
    private String remark;
}
