package com.washer.backend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.washer.backend.common.ApiResponse;
import com.washer.backend.dto.admin.AdminUserAssetOverview;
import com.washer.backend.dto.admin.AdminUserListItem;
import com.washer.backend.service.UserInfoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final UserInfoService userInfoService;

    public AdminUserController(UserInfoService userInfoService) {
        this.userInfoService = userInfoService;
    }

    @GetMapping
    public ApiResponse<Page<AdminUserListItem>> page(
        @RequestParam(defaultValue = "1") long page,
        @RequestParam(defaultValue = "10") long size,
        @RequestParam(required = false) String keyword
    ) {
        return ApiResponse.success(userInfoService.pageAdminUsers(page, size, keyword));
    }

    @GetMapping("/{id}")
    public ApiResponse<AdminUserAssetOverview> detail(@PathVariable Long id) {
        return ApiResponse.success(userInfoService.getAdminUserAssetOverview(id));
    }
}
