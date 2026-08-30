package com.washer.backend.dto.admin;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserCardPageItem {

    private Long id;
    private Long userId;
    private Long storeId;
    private String storeName;
    private Long cardProductId;
    private String cardType;
    private String sourceChannel;
    private String cardNo;
    private Integer totalTimes;
    private Integer usedTimes;
    private Integer remainingTimes;
    private LocalDateTime purchaseTime;
    private LocalDateTime effectiveTime;
    private LocalDateTime expireTime;
    private String status;
    private String externalOrderNo;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
