package com.washer.backend.dto.order;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class SimpleOrderCreateRequest {

    private Long userId;
    private Long storeId;
    private Long deviceId;
    private String payMode;
    private BigDecimal estimatedAmount;
    private BigDecimal finalAmount;
    private String remark;
}
