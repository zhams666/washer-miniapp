package com.washer.backend.dto.admin;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminRankingDurationItem {

    private Integer rank;
    private Long userId;
    private String userNo;
    private String nickname;
    private String mobile;
    private String avatarUrl;
    private Long realDurationSeconds;
    private Long realDurationMinutes;
    private String realDurationText;
    private Long durationSeconds;
    private Long durationMinutes;
    private String durationText;
    private Long displayAdjustmentSeconds;
    private Long displayAdjustmentMinutes;
    private String displayAdjustmentText;
    private Integer orderCount;
    private LocalDateTime latestEndTime;
}
