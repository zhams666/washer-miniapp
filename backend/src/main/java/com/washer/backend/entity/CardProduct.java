package com.washer.backend.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("card_product")
public class CardProduct {

    @TableId
    private Long id;

    private Long storeId;
    private String cardName;
    private String cardType;
    private Integer totalTimes;
    private BigDecimal salePrice;
    private Integer validDays;
    private Integer isNewUserOnly;
    private Integer purchaseLimit;
    private Integer status;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
