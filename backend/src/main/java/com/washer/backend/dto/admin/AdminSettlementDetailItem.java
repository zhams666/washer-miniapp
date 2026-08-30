package com.washer.backend.dto.admin;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AdminSettlementDetailItem {

    private Long id;
    private Long orderId;
    private String orderNo;
    private Long fromStoreId;
    private Long toStoreId;
    private BigDecimal principalAmount;
    private LocalDate bizDate;
    private String detailStatus;
}
