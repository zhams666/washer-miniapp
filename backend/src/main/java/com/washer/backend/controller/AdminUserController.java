package com.washer.backend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.washer.backend.common.ApiResponse;
import com.washer.backend.dto.admin.AdminUserAssetOverview;
import com.washer.backend.dto.admin.AdminUserCardAdjustResult;
import com.washer.backend.dto.admin.AdminUserCardDetail;
import com.washer.backend.dto.admin.AdminUserCardManualAddRequest;
import com.washer.backend.dto.admin.AdminUserCardManualReduceRequest;
import com.washer.backend.dto.admin.AdminUserCardPageItem;
import com.washer.backend.dto.admin.AdminUserCreateRequest;
import com.washer.backend.dto.admin.AdminUserListItem;
import com.washer.backend.dto.admin.AdminUserUpdateRequest;
import com.washer.backend.entity.UserInfo;
import com.washer.backend.service.UserInfoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    @PostMapping
    public ApiResponse<UserInfo> create(@RequestBody AdminUserCreateRequest request) {
        return ApiResponse.success("创建成功", userInfoService.createAdminUser(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<UserInfo> update(@PathVariable Long id, @RequestBody AdminUserUpdateRequest request) {
        return ApiResponse.success("更新成功", userInfoService.updateAdminUser(id, request));
    }

    @GetMapping("/{id}/cards")
    public ApiResponse<Page<AdminUserCardPageItem>> pageCards(
        @PathVariable Long id,
        @RequestParam(defaultValue = "1") long page,
        @RequestParam(defaultValue = "10") long size,
        @RequestParam(required = false) Long storeId,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String cardNo
    ) {
        return ApiResponse.success(userInfoService.pageAdminUserCards(id, page, size, storeId, status, cardNo));
    }

    @GetMapping("/{id}/cards/{cardId}")
    public ApiResponse<AdminUserCardDetail> cardDetail(@PathVariable Long id, @PathVariable Long cardId) {
        return ApiResponse.success(userInfoService.getAdminUserCardDetail(id, cardId));
    }

    @PostMapping("/{id}/cards/manual-add")
    public ApiResponse<AdminUserCardAdjustResult> manualAddCards(
        @PathVariable Long id,
        @RequestBody AdminUserCardManualAddRequest request
    ) {
        return ApiResponse.success("次卡已发放", userInfoService.manualAddUserCards(id, request));
    }

    @PostMapping("/{id}/cards/manual-reduce")
    public ApiResponse<AdminUserCardAdjustResult> manualReduceCards(
        @PathVariable Long id,
        @RequestBody AdminUserCardManualReduceRequest request
    ) {
        return ApiResponse.success("次卡已减少", userInfoService.manualReduceUserCards(id, request));
    }
}
