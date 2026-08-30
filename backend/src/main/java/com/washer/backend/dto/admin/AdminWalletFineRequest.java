package com.washer.backend.dto.admin;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class AdminWalletFineRequest {

    private Long userId;
    private Long storeId;
    private BigDecimal amount;
    private String remark;
}
