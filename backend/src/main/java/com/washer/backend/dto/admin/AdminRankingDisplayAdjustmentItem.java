package com.washer.backend.dto.admin;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AdminRankingDisplayAdjustmentItem {

    private Long id;
    private Long userId;
    private String scope;
    private String scopeName;
    private String userNo;
    private String nickname;
    private String mobile;
    private Long durationSeconds;
    private Long durationMinutes;
    private String durationText;
    private String remark;
    private LocalDateTime occurredAt;
    private LocalDateTime createdAt;
}
