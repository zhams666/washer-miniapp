package com.washer.backend.dto.admin;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AdminWalletRefundResult {

    private Long walletId;
    private Long userId;
    private Long storeId;
    private BigDecimal principalAmount;
}
