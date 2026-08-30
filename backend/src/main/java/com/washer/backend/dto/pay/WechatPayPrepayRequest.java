package com.washer.backend.dto.pay;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class WechatPayPrepayRequest {

    private String description;
    private String outTradeNo;
    private BigDecimal amount;
    private String openid;
    private String attach;
}
