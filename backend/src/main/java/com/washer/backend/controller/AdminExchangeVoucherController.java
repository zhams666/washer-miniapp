package com.washer.backend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.washer.backend.common.ApiResponse;
import com.washer.backend.dto.miniadmin.MiniAdminExchangeVoucherItem;
import com.washer.backend.service.StoreExchangeVoucherService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/exchange-vouchers")
public class AdminExchangeVoucherController {

    private final StoreExchangeVoucherService storeExchangeVoucherService;

    public AdminExchangeVoucherController(StoreExchangeVoucherService storeExchangeVoucherService) {
        this.storeExchangeVoucherService = storeExchangeVoucherService;
    }

    @GetMapping
    public ApiResponse<Page<MiniAdminExchangeVoucherItem>> pageExchangeVouchers(
        @RequestParam(defaultValue = "1") long page,
        @RequestParam(defaultValue = "10") long size,
        @RequestParam(required = false) Long storeId,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String keyword
    ) {
        return ApiResponse.success(
            storeExchangeVoucherService.pageAdminVouchers(page, size, storeId, status, keyword)
        );
    }
}
