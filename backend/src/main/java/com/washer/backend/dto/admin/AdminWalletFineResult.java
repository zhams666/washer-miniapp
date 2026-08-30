package com.washer.backend.dto.admin;

import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AdminWalletFineResult {

    private Long walletId;
    private Long userId;
    private Long storeId;
    private BigDecimal amount;
    private BigDecimal principalAmount;
    private BigDecimal giftAmount;
    private String bizActionNo;
    private List<String> transactionNos;
}
