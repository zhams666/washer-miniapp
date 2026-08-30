package com.washer.backend.dto.admin;

import java.time.LocalDate;
import lombok.Data;

@Data
public class AdminSettlementBillGenerateRequest {

    private String settlementPeriodType;
    private LocalDate startDate;
    private LocalDate endDate;
    private String remark;
}
