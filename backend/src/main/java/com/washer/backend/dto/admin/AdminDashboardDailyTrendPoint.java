package com.washer.backend.dto.admin;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardDailyTrendPoint {

    private LocalDate date;
    private String label;
    private BigDecimal rechargeAmount;
    private BigDecimal consumeAmount;
    private Long washCount;
    private Long cardUsageTimes;
    private Long groupVerifyCount;
}
