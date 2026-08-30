package com.washer.backend.dto.admin;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class AdminDashboardChannelMetric {

    private String channel;
    private Long orderCount = 0L;
    private Long cardCount = 0L;
    private BigDecimal payAmount = BigDecimal.ZERO;
}
