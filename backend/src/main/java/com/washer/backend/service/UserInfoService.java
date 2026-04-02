package com.washer.backend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.washer.backend.dto.admin.AdminUserAssetOverview;
import com.washer.backend.dto.admin.AdminUserListItem;
import com.baomidou.mybatisplus.extension.service.IService;
import com.washer.backend.entity.UserInfo;

public interface UserInfoService extends IService<UserInfo> {

    Page<AdminUserListItem> pageAdminUsers(long page, long size, String keyword);

    AdminUserAssetOverview getAdminUserAssetOverview(Long id);
}
