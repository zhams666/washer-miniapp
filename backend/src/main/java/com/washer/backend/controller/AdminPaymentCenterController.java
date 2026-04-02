package com.washer.backend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.washer.backend.common.ApiResponse;
import com.washer.backend.dto.admin.AdminCardUsageCenterItem;
import com.washer.backend.dto.admin.AdminPaymentDetailItem;
import com.washer.backend.dto.admin.AdminWalletTransactionCenterItem;
import com.washer.backend.service.AdminPaymentCenterService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminPaymentCenterController {

    private final AdminPaymentCenterService adminPaymentCenterService;

    public AdminPaymentCenterController(AdminPaymentCenterService adminPaymentCenterService) {
        this.adminPaymentCenterService = adminPaymentCenterService;
    }

    @GetMapping("/payment-details")
    public ApiResponse<Page<AdminPaymentDetailItem>> pagePaymentDetails(
        @RequestParam(defaultValue = "1") long page,
        @RequestParam(defaultValue = "10") long size,
        @RequestParam(required = false) String orderNo,
        @RequestParam(required = false) Long userId,
        @RequestParam(required = false) Long storeId,
        @RequestParam(required = false) String payMode,
        @RequestParam(required = false) String paymentStatus
    ) {
        return ApiResponse.success(
            adminPaymentCenterService.pagePaymentDetails(page, size, orderNo, userId, storeId, payMode, paymentStatus)
        );
    }

    @GetMapping("/wallet-transactions")
    public ApiResponse<Page<AdminWalletTransactionCenterItem>> pageWalletTransactions(
        @RequestParam(defaultValue = "1") long page,
        @RequestParam(defaultValue = "10") long size,
        @RequestParam(required = false) Long userId,
        @RequestParam(required = false) Long storeId,
        @RequestParam(required = false) String bizType,
        @RequestParam(required = false) String relatedOrderNo
    ) {
        return ApiResponse.success(
            adminPaymentCenterService.pageWalletTransactions(page, size, userId, storeId, bizType, relatedOrderNo)
        );
    }

    @GetMapping("/card-usages")
    public ApiResponse<Page<AdminCardUsageCenterItem>> pageCardUsages(
        @RequestParam(defaultValue = "1") long page,
        @RequestParam(defaultValue = "10") long size,
        @RequestParam(required = false) Long userId,
        @RequestParam(required = false) Long storeId,
        @RequestParam(required = false) String cardNo,
        @RequestParam(required = false) String orderNo
    ) {
        return ApiResponse.success(
            adminPaymentCenterService.pageCardUsages(page, size, userId, storeId, cardNo, orderNo)
        );
    }
}
