package com.washer.backend.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("store_exchange_voucher")
public class StoreExchangeVoucher {

    @TableId
    private Long id;

    private String batchNo;
    private String serialNo;
    private Long storeId;
    private BigDecimal amount;
    private String status;
    private Long redeemedUserId;
    private Long redeemedWalletTransactionId;
    private Long createdByStaffId;
    private String createdByRoleCode;
    private LocalDateTime redeemedAt;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
