package com.washer.backend.dto.costomer;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

@Data
public class CostomerPhoneLoginRequest {

    private String loginCode;
    private String phoneCode;
    private String mobile;
    private String verifyCode;
    @JsonAlias("openid")
    private String openId;
    @JsonAlias("nickName")
    private String nickname;
    private String avatarUrl;
    @JsonAlias("unionid")
    private String unionId;
}
