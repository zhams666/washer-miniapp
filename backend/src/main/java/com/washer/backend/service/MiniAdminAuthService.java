package com.washer.backend.service;

import com.washer.backend.dto.miniadmin.MiniAdminLoginRequest;
import com.washer.backend.dto.miniadmin.MiniAdminLoginResponse;
import com.washer.backend.dto.miniadmin.MiniAdminSessionContext;
import com.washer.backend.dto.miniadmin.MiniAdminStaffProfile;
import com.washer.backend.dto.miniadmin.MiniAdminStoreOption;
import java.util.List;

public interface MiniAdminAuthService {

    MiniAdminLoginResponse login(MiniAdminLoginRequest request);

    MiniAdminStaffProfile current(String token);

    MiniAdminSessionContext requireContext(String token);

    List<MiniAdminStoreOption> listAccessibleStores(String token);

    boolean canAccessStore(MiniAdminSessionContext context, Long storeId);

    boolean hasPermission(MiniAdminSessionContext context, String permission);
}
