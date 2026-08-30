package com.washer.backend.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("mini_admin_asset_operation")
public class MiniAdminAssetOperation {

    @TableId
    private Long id;

    private String operationNo;
    private String operationType;
    private String changeType;
    private Long userId;
    private Long storeId;
    private Long walletId;
    private Long userCardId;
    private String amountType;
    private BigDecimal principalAmount;
    private BigDecimal giftAmount;
    private BigDecimal totalAmount;
    private Integer cardDeltaTimes;
    private Long operatorStaffId;
    private String operatorRoleCode;
    private String remark;
    private LocalDateTime createdAt;
}
