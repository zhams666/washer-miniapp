package com.washer.backend.dto.admin;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardActivityItem {

    private String type;
    private String title;
    private String storeName;
    private String referenceNo;
    private BigDecimal amount;
    private Long times;
    private LocalDateTime occurredAt;
}
