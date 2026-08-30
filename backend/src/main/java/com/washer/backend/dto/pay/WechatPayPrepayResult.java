package com.washer.backend.dto.pay;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class WechatPayPrepayResult {

    private String prepayId;
    private WxPayRequestPaymentParams payParams;
}
