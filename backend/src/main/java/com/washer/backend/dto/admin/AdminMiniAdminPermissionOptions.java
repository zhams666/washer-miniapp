package com.washer.backend.dto.admin;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminMiniAdminPermissionOptions {

    private List<AdminPermissionOption> roles;
    private List<AdminPermissionOption> dataScopes;
    private List<AdminPermissionOption> permissions;
    private List<AdminStoreOption> stores;
}
