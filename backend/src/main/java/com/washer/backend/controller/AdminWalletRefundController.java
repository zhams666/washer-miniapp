package com.washer.backend.controller;

import com.washer.backend.common.ApiResponse;
import com.washer.backend.dto.admin.AdminWalletRefundRequest;
import com.washer.backend.dto.admin.AdminWalletRefundResult;
import com.washer.backend.service.AdminWalletRefundService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminWalletRefundController {

    private final AdminWalletRefundService adminWalletRefundService;

    public AdminWalletRefundController(AdminWalletRefundService adminWalletRefundService) {
        this.adminWalletRefundService = adminWalletRefundService;
    }

    @PostMapping("/wallet-refunds")
    public ApiResponse<AdminWalletRefundResult> manualRefund(@RequestBody AdminWalletRefundRequest request) {
        return ApiResponse.success(adminWalletRefundService.manualRefund(request));
    }
}
