package com.washer.backend.controller;

import com.washer.backend.common.ApiResponse;
import com.washer.backend.dto.admin.AdminWalletRechargeRequest;
import com.washer.backend.dto.admin.AdminWalletRechargeResult;
import com.washer.backend.service.AdminWalletRechargeService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminWalletRechargeController {

    private final AdminWalletRechargeService adminWalletRechargeService;

    public AdminWalletRechargeController(AdminWalletRechargeService adminWalletRechargeService) {
        this.adminWalletRechargeService = adminWalletRechargeService;
    }

    @PostMapping("/wallet-recharges")
    public ApiResponse<AdminWalletRechargeResult> manualRecharge(@RequestBody AdminWalletRechargeRequest request) {
        return ApiResponse.success(adminWalletRechargeService.manualRecharge(request));
    }
}
