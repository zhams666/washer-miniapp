package com.washer.backend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.washer.backend.common.ApiResponse;
import com.washer.backend.dto.miniadmin.MiniAdminExchangeVoucherBatchResult;
import com.washer.backend.dto.miniadmin.MiniAdminExchangeVoucherCreateRequest;
import com.washer.backend.dto.miniadmin.MiniAdminExchangeVoucherItem;
import com.washer.backend.dto.miniadmin.MiniAdminSessionContext;
import com.washer.backend.service.MiniAdminAuthService;
import com.washer.backend.service.StoreExchangeVoucherService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mini-admin/exchange-vouchers")
public class MiniAdminExchangeVoucherController {

    private final MiniAdminAuthService miniAdminAuthService;
    private final StoreExchangeVoucherService storeExchangeVoucherService;

    public MiniAdminExchangeVoucherController(
        MiniAdminAuthService miniAdminAuthService,
        StoreExchangeVoucherService storeExchangeVoucherService
    ) {
        this.miniAdminAuthService = miniAdminAuthService;
        this.storeExchangeVoucherService = storeExchangeVoucherService;
    }

    @PostMapping("/batches")
    public ApiResponse<MiniAdminExchangeVoucherBatchResult> createBatch(
        @RequestHeader(value = "X-Washer-Admin-Token", required = false) String token,
        @RequestBody MiniAdminExchangeVoucherCreateRequest request
    ) {
        MiniAdminSessionContext context = miniAdminAuthService.requireContext(token);
        return ApiResponse.success(storeExchangeVoucherService.createBatch(context, request));
    }

    @GetMapping
    public ApiResponse<Page<MiniAdminExchangeVoucherItem>> pageVouchers(
        @RequestHeader(value = "X-Washer-Admin-Token", required = false) String token,
        @RequestParam(defaultValue = "1") long page,
        @RequestParam(defaultValue = "20") long size,
        @RequestParam(required = false) Long storeId,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String keyword
    ) {
        MiniAdminSessionContext context = miniAdminAuthService.requireContext(token);
        return ApiResponse.success(
            storeExchangeVoucherService.pageVouchers(context, page, size, storeId, status, keyword)
        );
    }
}
