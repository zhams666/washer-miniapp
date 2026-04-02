package com.washer.backend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.washer.backend.common.ApiResponse;
import com.washer.backend.dto.admin.AdminOrderDetail;
import com.washer.backend.dto.admin.AdminOrderListItem;
import com.washer.backend.service.WashOrderService;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/orders")
public class AdminOrderController {

    private final WashOrderService washOrderService;

    public AdminOrderController(WashOrderService washOrderService) {
        this.washOrderService = washOrderService;
    }

    @GetMapping
    public ApiResponse<Page<AdminOrderListItem>> page(
        @RequestParam(defaultValue = "1") long page,
        @RequestParam(defaultValue = "10") long size,
        @RequestParam(required = false) Long storeId,
        @RequestParam(required = false) String orderStatus,
        @RequestParam(required = false) String paymentStatus,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime
    ) {
        return ApiResponse.success(
            washOrderService.pageAdminOrders(page, size, storeId, orderStatus, paymentStatus, keyword, startTime, endTime)
        );
    }

    @GetMapping("/{id}")
    public ApiResponse<AdminOrderDetail> detail(@PathVariable Long id) {
        return ApiResponse.success(washOrderService.getAdminOrderDetail(id));
    }
}
