package com.washer.backend.dto.admin;

import com.washer.backend.entity.WashOrderPaymentDetail;
import com.washer.backend.entity.WashOrderStatusLog;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AdminOrderDetail {

    private Long id;
    private String orderNo;
    private Long userId;
    private Long storeId;
    private String storeName;
    private Long deviceId;
    private String deviceCode;
    private String deviceName;
    private String deviceStatus;
    private String orderSource;
    private String payMode;
    private String paymentStatus;
    private String paymentStatusDesc;
    private String orderStatus;
    private BigDecimal estimatedAmount;
    private BigDecimal finalAmount;
    private BigDecimal paidAmount;
    private BigDecimal refundAmount;
    private Integer isFirstPeriodDiscountUsed;
    private BigDecimal firstPeriodDiscountAmount;
    private String firstPeriodDiscountDesc;
    private Long cardUsageId;
    private Integer cardDeductTimes;
    private String remark;
    private String abnormalReason;
    private LocalDateTime createdAt;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime settleTime;
    private List<WashOrderStatusLog> statusLogs;
    private List<WashOrderPaymentDetail> paymentDetails;
}
