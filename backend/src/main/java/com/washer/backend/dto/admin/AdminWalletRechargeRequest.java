package com.washer.backend.dto.admin;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class AdminWalletRechargeRequest {

    private Long userId;
    private Long storeId;
    private Long rechargeProductId;
    private BigDecimal payAmount;
    private BigDecimal principalAmount;
    private BigDecimal giftAmount;
    private String remark;
}
