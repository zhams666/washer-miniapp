package com.washer.backend.dto.admin;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardKpiItem {

    private String key;
    private String title;
    private BigDecimal amount;
    private Long count;
    private String unit;
    private String description;
}
