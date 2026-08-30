package com.washer.backend.controller;

import com.washer.backend.common.ApiResponse;
import com.washer.backend.dto.miniadmin.MiniAdminLoginRequest;
import com.washer.backend.dto.miniadmin.MiniAdminLoginResponse;
import com.washer.backend.dto.miniadmin.MiniAdminStaffProfile;
import com.washer.backend.dto.miniadmin.MiniAdminStoreOption;
import com.washer.backend.service.MiniAdminAuthService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mini-admin")
public class MiniAdminAuthController {

    private final MiniAdminAuthService miniAdminAuthService;

    public MiniAdminAuthController(MiniAdminAuthService miniAdminAuthService) {
        this.miniAdminAuthService = miniAdminAuthService;
    }

    @PostMapping("/auth/login")
    public ApiResponse<MiniAdminLoginResponse> login(@RequestBody MiniAdminLoginRequest request) {
        return ApiResponse.success(miniAdminAuthService.login(request));
    }

    @GetMapping("/auth/current")
    public ApiResponse<MiniAdminStaffProfile> current(
        @RequestHeader(value = "X-Washer-Admin-Token", required = false) String token
    ) {
        return ApiResponse.success(miniAdminAuthService.current(token));
    }

    @GetMapping("/stores/options")
    public ApiResponse<List<MiniAdminStoreOption>> storeOptions(
        @RequestHeader(value = "X-Washer-Admin-Token", required = false) String token
    ) {
        return ApiResponse.success(miniAdminAuthService.listAccessibleStores(token));
    }
}
