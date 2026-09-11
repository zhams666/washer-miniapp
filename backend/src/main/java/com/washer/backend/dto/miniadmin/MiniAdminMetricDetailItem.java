package com.washer.backend.dto.miniadmin;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MiniAdminMetricDetailItem {

    private String type;
    private String title;
    private String referenceNo;
    private Long userId;
    private String userNickname;
    private String userMobile;
    private Long storeId;
    private String storeName;
    private BigDecimal amount;
    private Long count;
    private String status;
    private LocalDateTime occurredAt;
}
