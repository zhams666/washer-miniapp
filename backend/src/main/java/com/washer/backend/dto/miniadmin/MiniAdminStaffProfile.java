package com.washer.backend.dto.miniadmin;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MiniAdminStaffProfile {

    private Long id;
    private Long franchiseeId;
    private String franchiseeName;
    private String staffNo;
    private String staffName;
    private String mobile;
    private String roleCode;
    private String roleName;
    private String dataScope;
    private Boolean platformScope;
    private List<String> permissions;
    private List<MiniAdminStoreOption> stores;
}
