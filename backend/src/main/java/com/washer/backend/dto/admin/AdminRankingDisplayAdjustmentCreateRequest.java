package com.washer.backend.dto.admin;

import lombok.Data;

@Data
public class AdminRankingDisplayAdjustmentCreateRequest {

    private Long userId;
    private String scope;
    private Long durationMinutes;
    private Long durationSeconds;
    private String remark;
}
