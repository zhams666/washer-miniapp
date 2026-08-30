package com.washer.backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "wechat.pay")
public class WechatPayProperties {

    private boolean enabled = false;
    private String appId;
    private String mchId;
    private String merchantSerialNo;
    private String privateKeyPath;
    private String apiV3Key;
    private String notifyUrl;
    private String platformCertificatePath;
    private String baseUrl = "https://api.mch.weixin.qq.com";
}
