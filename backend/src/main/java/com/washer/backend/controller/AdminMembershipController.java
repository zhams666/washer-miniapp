package com.washer.backend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.washer.backend.common.ApiResponse;
import com.washer.backend.entity.MembershipPlan;
import com.washer.backend.entity.MembershipSetting;
import com.washer.backend.entity.MembershipOrder;
import com.washer.backend.service.MembershipService;
import java.util.Map;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/membership")
public class AdminMembershipController {

    private final MembershipService membershipService;

    public AdminMembershipController(MembershipService membershipService) {
        this.membershipService = membershipService;
    }

    @GetMapping("/settings")
    public ApiResponse<MembershipSetting> settings() {
        return ApiResponse.success(membershipService.getSettings());
    }

    @PutMapping("/settings")
    public ApiResponse<MembershipSetting> saveSettings(@RequestBody MembershipSetting settings) {
        return ApiResponse.success("会员日设置已保存", membershipService.saveSettings(settings));
    }

    @GetMapping("/plans")
    public ApiResponse<java.util.List<MembershipPlan>> plans() {
        return ApiResponse.success(membershipService.listAllPlans());
    }

    @PostMapping("/plans")
    public ApiResponse<MembershipPlan> createPlan(@RequestBody Map<String, Object> payload) {
        return ApiResponse.success("会员方案已创建", membershipService.savePlan(null, payload));
    }

    @PutMapping("/plans/{id}")
    public ApiResponse<MembershipPlan> updatePlan(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        return ApiResponse.success("会员方案已更新", membershipService.savePlan(id, payload));
    }

    @DeleteMapping("/plans/{id}")
    public ApiResponse<Void> disablePlan(@PathVariable Long id) {
        membershipService.disablePlan(id);
        return ApiResponse.success("会员方案已下架", null);
    }

    @GetMapping("/orders")
    public ApiResponse<Page<MembershipOrder>> orders(
        @RequestParam(defaultValue = "1") long page,
        @RequestParam(defaultValue = "10") long size,
        @RequestParam(required = false) Long userId,
        @RequestParam(required = false) String status
    ) {
        return ApiResponse.success(membershipService.pageOrders(page, size, userId, status));
    }
}
