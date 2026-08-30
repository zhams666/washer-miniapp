package com.washer.backend.dto.miniadmin;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MiniAdminRecentActivityItem {

    private String type;
    private String title;
    private Long storeId;
    private String storeName;
    private String referenceNo;
    private BigDecimal amount;
    private Long times;
    private LocalDateTime occurredAt;
}
