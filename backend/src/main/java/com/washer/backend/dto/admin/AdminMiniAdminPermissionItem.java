package com.washer.backend.dto.admin;

import com.washer.backend.dto.miniadmin.MiniAdminStoreOption;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminMiniAdminPermissionItem {

    private Boolean exists;
    private Long userId;
    private String userNo;
    private String nickname;
    private String mobile;
    private String openId;
    private Long staffId;
    private String staffNo;
    private String staffName;
    private String roleCode;
    private String roleName;
    private String dataScope;
    private String dataScopeName;
    private Integer status;
    private String remark;
    private List<Long> storeIds;
    private List<MiniAdminStoreOption> stores;
    private List<String> permissions;
    private LocalDateTime updatedAt;
}
