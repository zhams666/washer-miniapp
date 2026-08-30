package com.washer.backend.dto.miniadmin;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MiniAdminAssetOperationResult {

    private String operationNo;
    private String operationType;
    private Long userId;
    private Long storeId;
    private Long walletId;
    private Long userCardId;
    private BigDecimal principalAmount;
    private BigDecimal giftAmount;
    private BigDecimal totalAmount;
    private Integer cardDeltaTimes;
    private String remark;
}
