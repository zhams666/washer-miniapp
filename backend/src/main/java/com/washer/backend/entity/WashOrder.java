package com.washer.backend.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("wash_order")
public class WashOrder {

    @TableId
    private Long id;

    private String orderNo;
    private Long userId;
    private Long storeId;
    private Long bayId;
    private Long deviceId;
    private Long pricingRuleId;
    private String orderSource;
    private String orderStatus;
    private String deviceStatusSnapshot;
    private String payMode;
    private String paymentStatus;
    private String paymentStatusDesc;
    private String refundStatusSnapshot;
    private String pricingSnapshot;
    private Integer pricingSnapshotVersion;
    private Long cardUsageId;
    private Integer cardDeductTimes;
    private BigDecimal estimatedAmount;
    private BigDecimal finalAmount;
    private BigDecimal paidAmount;
    private BigDecimal refundAmount;
    private Integer isFirstPeriodDiscountUsed;
    private BigDecimal firstPeriodDiscountAmount;
    private String startCommandNo;
    private String pauseCommandNo;
    private String stopCommandNo;
    private String cancelCommandNo;
    private LocalDateTime startTime;
    private LocalDateTime pauseTime;
    private LocalDateTime endTime;
    private LocalDateTime cancelTime;
    private LocalDateTime settleTime;
    private String abnormalReason;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
