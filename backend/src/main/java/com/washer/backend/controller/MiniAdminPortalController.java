package com.washer.backend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.washer.backend.common.ApiResponse;
import com.washer.backend.dto.device.DeviceSimpleItem;
import com.washer.backend.dto.miniadmin.MiniAdminDashboardOverview;
import com.washer.backend.dto.miniadmin.MiniAdminOrderItem;
import com.washer.backend.dto.miniadmin.MiniAdminOperationOverview;
import com.washer.backend.dto.miniadmin.MiniAdminSessionContext;
import com.washer.backend.service.MiniAdminAuthService;
import com.washer.backend.service.MiniAdminPortalService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mini-admin")
public class MiniAdminPortalController {

    private final MiniAdminAuthService miniAdminAuthService;
    private final MiniAdminPortalService miniAdminPortalService;

    public MiniAdminPortalController(
        MiniAdminAuthService miniAdminAuthService,
        MiniAdminPortalService miniAdminPortalService
    ) {
        this.miniAdminAuthService = miniAdminAuthService;
        this.miniAdminPortalService = miniAdminPortalService;
    }

    @GetMapping("/dashboard")
    public ApiResponse<MiniAdminDashboardOverview> dashboard(
        @RequestHeader(value = "X-Washer-Admin-Token", required = false) String token,
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate bizDate,
        @RequestParam(required = false) Long storeId
    ) {
        MiniAdminSessionContext context = miniAdminAuthService.requireContext(token);
        return ApiResponse.success(miniAdminPortalService.getDashboard(context, bizDate, storeId));
    }

    @GetMapping("/operation/overview")
    public ApiResponse<MiniAdminOperationOverview> operationOverview(
        @RequestHeader(value = "X-Washer-Admin-Token", required = false) String token,
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate bizDate,
        @RequestParam(required = false) Long storeId
    ) {
        MiniAdminSessionContext context = miniAdminAuthService.requireContext(token);
        return ApiResponse.success(miniAdminPortalService.getOperationOverview(context, bizDate, storeId));
    }

    @GetMapping("/devices")
    public ApiResponse<List<DeviceSimpleItem>> devices(
        @RequestHeader(value = "X-Washer-Admin-Token", required = false) String token,
        @RequestParam(required = false) Long storeId,
        @RequestParam(required = false) String keyword
    ) {
        MiniAdminSessionContext context = miniAdminAuthService.requireContext(token);
        return ApiResponse.success(miniAdminPortalService.listDevices(context, storeId, keyword));
    }

    @PostMapping("/devices/{id}/start")
    public ApiResponse<DeviceSimpleItem> startDevice(
        @RequestHeader(value = "X-Washer-Admin-Token", required = false) String token,
        @PathVariable Long id
    ) {
        MiniAdminSessionContext context = miniAdminAuthService.requireContext(token);
        return ApiResponse.success(miniAdminPortalService.startDevice(context, id));
    }

    @PostMapping("/devices/{id}/stop")
    public ApiResponse<DeviceSimpleItem> stopDevice(
        @RequestHeader(value = "X-Washer-Admin-Token", required = false) String token,
        @PathVariable Long id
    ) {
        MiniAdminSessionContext context = miniAdminAuthService.requireContext(token);
        return ApiResponse.success(miniAdminPortalService.stopDevice(context, id));
    }

    @GetMapping("/orders")
    public ApiResponse<Page<MiniAdminOrderItem>> orders(
        @RequestHeader(value = "X-Washer-Admin-Token", required = false) String token,
        @RequestParam(defaultValue = "1") long page,
        @RequestParam(defaultValue = "10") long size,
        @RequestParam(required = false) Long storeId,
        @RequestParam(required = false) String orderStatus,
        @RequestParam(required = false) String paymentStatus,
        @RequestParam(required = false) String keyword
    ) {
        MiniAdminSessionContext context = miniAdminAuthService.requireContext(token);
        return ApiResponse.success(
            miniAdminPortalService.pageOrders(context, page, size, storeId, orderStatus, paymentStatus, keyword)
        );
    }
}
