package com.washer.backend.dto.admin;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AdminUserWalletAsset {

    private Long id;
    private Long storeId;
    private String storeName;
    private BigDecimal principalBalance;
    private BigDecimal availablePrincipalBalance;
    private BigDecimal giftBalance;
    private BigDecimal availableGiftBalance;
    private Integer status;
    private LocalDateTime updatedAt;
}
