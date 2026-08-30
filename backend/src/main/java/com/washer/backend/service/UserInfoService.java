package com.washer.backend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
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

public interface UserInfoService extends IService<UserInfo> {

    Page<AdminUserListItem> pageAdminUsers(long page, long size, String keyword);

    AdminUserAssetOverview getAdminUserAssetOverview(Long id);

    UserInfo createAdminUser(AdminUserCreateRequest request);

    UserInfo updateAdminUser(Long id, AdminUserUpdateRequest request);

    Page<AdminUserCardPageItem> pageAdminUserCards(
        Long userId,
        long page,
        long size,
        Long storeId,
        String status,
        String cardNo
    );

    AdminUserCardDetail getAdminUserCardDetail(Long userId, Long cardId);

    AdminUserCardAdjustResult manualAddUserCards(Long userId, AdminUserCardManualAddRequest request);

    AdminUserCardAdjustResult manualReduceUserCards(Long userId, AdminUserCardManualReduceRequest request);
}
