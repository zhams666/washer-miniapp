package com.washer.backend.dto.admin;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AdminUserCardAsset {

    private Long id;
    private Long storeId;
    private String storeName;
    private String cardNo;
    private String cardType;
    private String sourceChannel;
    private Integer totalTimes;
    private Integer usedTimes;
    private Integer remainingTimes;
    private String status;
    private LocalDateTime effectiveTime;
    private LocalDateTime expireTime;
    private LocalDateTime updatedAt;
}
