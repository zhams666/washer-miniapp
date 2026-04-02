package com.washer.backend.dto.admin;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AdminUserCardUsageItem {

    private Long id;
    private String usageNo;
    private Long userCardId;
    private Long storeId;
    private String storeName;
    private Long orderId;
    private String orderNo;
    private Integer usedTimes;
    private LocalDateTime usageTime;
    private LocalDateTime createdAt;
}
