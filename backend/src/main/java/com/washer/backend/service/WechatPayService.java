package com.washer.backend.service;

import com.washer.backend.dto.pay.WechatPayNotifyResult;
import com.washer.backend.dto.pay.WechatPayOrderQueryResult;
import com.washer.backend.dto.pay.WechatPayPrepayRequest;
import com.washer.backend.dto.pay.WechatPayPrepayResult;
import java.util.Map;

public interface WechatPayService {

    boolean isEnabled();

    WechatPayPrepayResult createJsapiPrepay(WechatPayPrepayRequest request);

    WechatPayOrderQueryResult queryOrderByOutTradeNo(String outTradeNo);

    WechatPayNotifyResult parseNotify(Map<String, String> headers, String body);
}
