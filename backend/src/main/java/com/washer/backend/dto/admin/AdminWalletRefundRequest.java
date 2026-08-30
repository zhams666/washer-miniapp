package com.washer.backend.dto.admin;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class AdminWalletRefundRequest {

    private Long userId;
    private Long storeId;
    private BigDecimal principalAmount;
    private String remark;
}
