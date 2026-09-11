package com.washer.backend.dto.miniadmin;

import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MiniAdminExchangeVoucherBatchResult {

    private String batchNo;
    private Long storeId;
    private String storeName;
    private BigDecimal amount;
    private Integer count;
    private List<MiniAdminExchangeVoucherItem> vouchers;
}
