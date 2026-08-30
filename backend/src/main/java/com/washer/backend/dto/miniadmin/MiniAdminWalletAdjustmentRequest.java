package com.washer.backend.dto.miniadmin;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class MiniAdminWalletAdjustmentRequest {

    private Long userId;
    private Long storeId;
    private String changeType;
    private BigDecimal principalAmount;
    private BigDecimal giftAmount;
    private String remark;
}
