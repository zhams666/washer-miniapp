package com.washer.backend.dto.miniadmin;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MiniAdminStoreRankingItem {

    private Long storeId;
    private Long franchiseeId;
    private String storeName;
    private String franchiseeName;
    private Long washCount;
    private BigDecimal consumeAmount;
    private BigDecimal rechargeAmount;
    private Long abnormalDeviceCount;
    private Long totalDeviceCount;
}
