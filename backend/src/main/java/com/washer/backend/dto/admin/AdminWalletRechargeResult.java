package com.washer.backend.dto.admin;

import com.washer.backend.dto.pay.WxPayRequestPaymentParams;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminWalletRechargeResult {

    private String rechargeOrderNo;
    private String paymentNo;
    private Long walletId;
    private Long userId;
    private Long storeId;
    private Long rechargeProductId;
    private BigDecimal payAmount;
    private BigDecimal principalAmount;
    private BigDecimal giftAmount;
    private String payStatus;
    private WxPayRequestPaymentParams payParams;
    private LocalDateTime expireTime;
    private String failReason;
}
