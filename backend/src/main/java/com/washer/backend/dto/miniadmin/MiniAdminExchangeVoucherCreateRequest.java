package com.washer.backend.dto.miniadmin;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class MiniAdminExchangeVoucherCreateRequest {

    private Long storeId;
    private BigDecimal amount;
    private Integer count;
    private String remark;
}
