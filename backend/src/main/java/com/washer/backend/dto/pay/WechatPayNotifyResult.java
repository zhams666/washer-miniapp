package com.washer.backend.dto.pay;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class WechatPayNotifyResult {

    private String notifyId;
    private String eventType;
    private String outTradeNo;
    private String transactionId;
    private String tradeState;
    private Integer payerTotal;
    private LocalDateTime successTime;
    private String rawContent;
}
