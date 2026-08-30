package com.washer.backend.dto.pricing;

import java.math.BigDecimal;

public record WashPricingSnapshot(
    Long pricingRuleId,
    BigDecimal basePrice,
    Integer baseMinutes,
    BigDecimal overtimePricePerMinute,
    String pricingRuleText,
    Integer ruleVersion,
    Boolean vipMonthlyPricing,
    Boolean memberDayDiscountApplied,
    Integer memberDayFirstMinutes,
    BigDecimal memberDayFirstPrice
) {
}
