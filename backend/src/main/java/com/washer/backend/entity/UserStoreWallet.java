package com.washer.backend.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("user_store_wallet")
public class UserStoreWallet {

    @TableId
    private Long id;

    private Long userId;
    private Long storeId;
    private BigDecimal principalBalance;
    private BigDecimal availablePrincipalBalance;
    private BigDecimal frozenPrincipalBalance;
    private BigDecimal giftBalance;
    private BigDecimal availableGiftBalance;
    private BigDecimal frozenGiftBalance;
    private BigDecimal totalRechargePrincipal;
    private BigDecimal totalRechargeGift;
    private BigDecimal totalConsumePrincipal;
    private BigDecimal totalConsumeGift;
    private BigDecimal totalRefundPrincipal;
    private Integer status;
    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
