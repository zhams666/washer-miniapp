package com.washer.backend.dto.admin;

import java.util.List;
import lombok.Data;

@Data
public class AdminMiniAdminPermissionRequest {

    private String userNo;
    private String staffName;
    private String roleCode;
    private String dataScope;
    private Long franchiseeId;
    private List<Long> storeIds;
    private Integer status;
    private String remark;
}
