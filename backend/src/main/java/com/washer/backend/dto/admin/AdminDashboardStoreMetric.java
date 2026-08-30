package com.washer.backend.dto.admin;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class AdminDashboardStoreMetric {

    private Long storeId;
    private String storeName;
    private Long cardUsageTimes = 0L;
    private BigDecimal walletConsumeAmount = BigDecimal.ZERO;
    private BigDecimal walletRechargeAmount = BigDecimal.ZERO;
    private BigDecimal cardPurchaseAmount = BigDecimal.ZERO;
}
