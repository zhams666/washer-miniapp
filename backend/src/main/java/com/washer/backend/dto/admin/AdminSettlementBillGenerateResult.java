package com.washer.backend.dto.admin;

import java.time.LocalDate;
import lombok.Data;

@Data
public class AdminSettlementBillGenerateResult {

    private int generatedCount;
    private int updatedDetailCount;
    private String settlementPeriodType;
    private LocalDate startDate;
    private LocalDate endDate;
}
