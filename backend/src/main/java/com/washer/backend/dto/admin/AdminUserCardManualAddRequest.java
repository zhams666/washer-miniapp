package com.washer.backend.dto.admin;

import lombok.Data;

@Data
public class AdminUserCardManualAddRequest {

    private Long storeId;
    private Integer count;
    private String effectiveTime;
    private String expireTime;
    private String remark;
}
