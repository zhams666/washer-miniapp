package com.washer.backend.dto.admin;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AdminOrderListItem {

    private Long id;
    private String orderNo;
    private Long userId;
    private Long storeId;
    private String storeName;
    private Long deviceId;
    private String deviceName;
    private String payMode;
    private String paymentStatus;
    private String orderStatus;
    private BigDecimal finalAmount;
    private BigDecimal paidAmount;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
