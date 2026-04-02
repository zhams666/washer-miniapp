package com.washer.backend.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("user_card")
public class UserCard {

    @TableId
    private Long id;

    private Long userId;
    private Long storeId;
    private Long cardProductId;
    private String cardType;
    private String sourceChannel;
    private String cardNo;
    private Integer totalTimes;
    private Integer usedTimes;
    private Integer remainingTimes;
    private LocalDateTime purchaseTime;
    private LocalDateTime effectiveTime;
    private LocalDateTime expireTime;
    private String status;
    private String externalOrderNo;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
