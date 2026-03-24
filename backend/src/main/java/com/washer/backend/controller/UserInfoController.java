package com.washer.backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.washer.backend.common.ApiResponse;
import com.washer.backend.entity.UserInfo;
import com.washer.backend.service.UserInfoService;
import java.util.UUID;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserInfoController {

    private final UserInfoService userInfoService;

    public UserInfoController(UserInfoService userInfoService) {
        this.userInfoService = userInfoService;
    }

    @GetMapping
    public ApiResponse<Page<UserInfo>> page(
        @RequestParam(defaultValue = "1") long page,
        @RequestParam(defaultValue = "10") long size,
        @RequestParam(required = false) String keyword
    ) {
        LambdaQueryWrapper<UserInfo> wrapper = new LambdaQueryWrapper<UserInfo>()
            .orderByDesc(UserInfo::getId);

        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w
                .like(UserInfo::getNickname, keyword)
                .or()
                .like(UserInfo::getMobile, keyword)
                .or()
                .like(UserInfo::getUserNo, keyword));
        }

        return ApiResponse.success(userInfoService.page(new Page<>(page, size), wrapper));
    }

    @GetMapping("/{id}")
    public ApiResponse<UserInfo> getById(@PathVariable Long id) {
        UserInfo user = userInfoService.getById(id);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        return ApiResponse.success(user);
    }

    @PostMapping
    public ApiResponse<UserInfo> create(@RequestBody UserInfo userInfo) {
        if (!StringUtils.hasText(userInfo.getUserNo())) {
            userInfo.setUserNo("U" + UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        }
        if (!StringUtils.hasText(userInfo.getNickname())) {
            userInfo.setNickname("微信用户");
        }
        if (userInfo.getUserStatus() == null) {
            userInfo.setUserStatus(1);
        }
        if (!StringUtils.hasText(userInfo.getRegisterSource())) {
            userInfo.setRegisterSource("admin");
        }
        if (userInfo.getIsMember() == null) {
            userInfo.setIsMember(0);
        }
        if (!StringUtils.hasText(userInfo.getMemberLevel())) {
            userInfo.setMemberLevel("normal");
        }
        userInfoService.save(userInfo);
        return ApiResponse.success("创建成功", userInfo);
    }

    @PutMapping("/{id}")
    public ApiResponse<UserInfo> update(@PathVariable Long id, @RequestBody UserInfo userInfo) {
        userInfo.setId(id);
        boolean updated = userInfoService.updateById(userInfo);
        if (!updated) {
            throw new IllegalArgumentException("用户不存在或更新失败");
        }
        return ApiResponse.success("更新成功", userInfoService.getById(id));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Boolean> delete(@PathVariable Long id) {
        boolean removed = userInfoService.removeById(id);
        if (!removed) {
            throw new IllegalArgumentException("用户不存在或删除失败");
        }
        return ApiResponse.success("删除成功", true);
    }
}
