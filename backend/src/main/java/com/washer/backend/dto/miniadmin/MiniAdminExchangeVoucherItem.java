package com.washer.backend.dto.miniadmin;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MiniAdminExchangeVoucherItem {

    private Long id;
    private String batchNo;
    private String serialNo;
    private Long storeId;
    private String storeName;
    private BigDecimal amount;
    private String status;
    private Long redeemedUserId;
    private String redeemedUserNickname;
    private String redeemedUserMobile;
    private String redeemTransactionNo;
    private LocalDateTime redeemedAt;
    private String remark;
    private LocalDateTime createdAt;
}
