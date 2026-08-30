package com.washer.backend.dto.miniadmin;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MiniAdminOrderItem {

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
    private LocalDateTime createdAt;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
