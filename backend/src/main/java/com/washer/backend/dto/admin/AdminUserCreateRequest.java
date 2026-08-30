package com.washer.backend.dto.admin;

import lombok.Data;

@Data
public class AdminUserCreateRequest {

    private String userNo;
    private String nickname;
    private String realName;
    private String mobile;
    private Integer userStatus;
    private String remark;
}
