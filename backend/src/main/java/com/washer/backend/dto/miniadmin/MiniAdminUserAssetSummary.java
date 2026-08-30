package com.washer.backend.dto.miniadmin;

import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MiniAdminUserAssetSummary {

    private Long userId;
    private String userNo;
    private String nickname;
    private String realName;
    private String mobile;
    private Long storeId;
    private String storeName;
    private Long walletId;
    private BigDecimal principalBalance;
    private BigDecimal giftBalance;
    private BigDecimal totalBalance;
    private Integer remainingCardTimes;
    private List<MiniAdminUserCardItem> cards;
}
