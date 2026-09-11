package com.washer.backend.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("ranking_display_adjustment")
public class RankingDisplayAdjustment {

    @TableId
    private Long id;

    private Long userId;
    private String scope;
    private Long displayDurationSeconds;
    private String remark;
    private LocalDateTime occurredAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
