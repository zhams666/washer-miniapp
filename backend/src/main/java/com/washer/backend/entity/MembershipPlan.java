package com.washer.backend.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("membership_plan")
public class MembershipPlan {

    @TableId
    private Long id;

    private String planCode;
    private String planName;
    private String planType;
    private Integer durationMonths;
    private BigDecimal price;
    private String benefitText;
    private Integer status;
    private Integer sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
