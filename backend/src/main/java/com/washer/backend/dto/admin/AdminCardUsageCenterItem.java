package com.washer.backend.dto.admin;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AdminCardUsageCenterItem {

    private Long id;
    private String usageNo;
    private Long userCardId;
    private String cardNo;
    private Long userId;
    private Long storeId;
    private String storeName;
    private Long orderId;
    private String orderNo;
    private Integer usedTimes;
    private LocalDateTime usageTime;
    private String remark;
    private LocalDateTime createdAt;
}
