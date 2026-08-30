package com.washer.backend.dto.admin;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardHourlyPoint {

    private Integer hour;
    private String label;
    private Long cardUsageTimes;
    private BigDecimal walletConsumeAmount;
    private BigDecimal walletRechargeAmount;
    private BigDecimal cardPurchaseAmount;
}
