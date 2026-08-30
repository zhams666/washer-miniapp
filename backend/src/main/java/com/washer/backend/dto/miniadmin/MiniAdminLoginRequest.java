package com.washer.backend.dto.miniadmin;

import lombok.Data;

@Data
public class MiniAdminLoginRequest {

    private String code;
    private String openId;
}
