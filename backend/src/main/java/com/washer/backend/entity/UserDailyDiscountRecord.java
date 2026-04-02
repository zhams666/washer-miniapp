package com.washer.backend.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("user_daily_discount_record")
public class UserDailyDiscountRecord {

    @TableId
    private Long id;

    private Long userId;
    private LocalDate discountDate;
    private String discountType;
    private String discountScope;
    private Long storeId;
    private Long scopeStoreId;
    private Long orderId;
    private String orderNo;
    private BigDecimal discountAmount;
    private LocalDateTime createdAt;
}
