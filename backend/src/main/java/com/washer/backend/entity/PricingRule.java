package com.washer.backend.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("pricing_rule")
public class PricingRule {

    @TableId
    private Long id;

    private Long storeId;
    private String ruleName;
    private Integer memberPriceEnabled;
    private Integer freeMinutes;
    private Integer firstPeriodMinutes;
    private BigDecimal firstPeriodPrice;
    private BigDecimal extraPricePerMinute;
    private BigDecimal memberFirstPeriodPrice;
    private BigDecimal memberExtraPricePerMinute;
    private String firstPeriodDiscountLimitType;
    private Integer isDefault;
    private Integer status;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
