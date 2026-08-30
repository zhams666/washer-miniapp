package com.washer.backend.dto.miniadmin;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class MiniAdminWalletFineRequest {

    private Long userId;
    private Long storeId;
    private BigDecimal amount;
    private String remark;
}
