package com.washer.backend.dto.admin;

import lombok.Data;

@Data
public class AdminUserUpdateRequest {

    private String nickname;
    private String realName;
    private String mobile;
    private Integer userStatus;
    private String remark;
}
