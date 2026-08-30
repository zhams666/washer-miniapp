package com.washer.backend.dto.admin;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardMetric {

    private String key;
    private String title;
    private BigDecimal amount;
    private Long count;
    private Long times;
    private BigDecimal secondaryAmount;
}
