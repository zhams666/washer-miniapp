package com.washer.backend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.washer.backend.common.ApiResponse;
import com.washer.backend.dto.order.SimpleOrderCreateRequest;
import com.washer.backend.dto.order.SimpleOrderItem;
import com.washer.backend.entity.WashOrder;
import com.washer.backend.entity.WashOrderStatusLog;
import com.washer.backend.service.WashOrderService;
import java.util.List;
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
@RequestMapping("/api/orders")
public class WashOrderController {

    private final WashOrderService washOrderService;

    public WashOrderController(WashOrderService washOrderService) {
        this.washOrderService = washOrderService;
    }

    @GetMapping
    public ApiResponse<Page<WashOrder>> page(
        @RequestParam(defaultValue = "1") long page,
        @RequestParam(defaultValue = "10") long size,
        @RequestParam(required = false) Long userId,
        @RequestParam(required = false) Long storeId,
        @RequestParam(required = false) String orderStatus,
        @RequestParam(required = false) String keyword
    ) {
        return ApiResponse.success(
            washOrderService.lambdaQuery()
                .eq(userId != null, WashOrder::getUserId, userId)
                .eq(storeId != null, WashOrder::getStoreId, storeId)
                .eq(orderStatus != null && !orderStatus.isBlank(), WashOrder::getOrderStatus, orderStatus)
                .and(keyword != null && !keyword.isBlank(), w -> w.like(WashOrder::getOrderNo, keyword).or().like(WashOrder::getRemark, keyword))
                .orderByDesc(WashOrder::getId)
                .page(new Page<>(page, size))
        );
    }

    @GetMapping("/simple-list")
    public ApiResponse<List<SimpleOrderItem>> simpleList(
        @RequestParam(required = false) Long userId,
        @RequestParam(defaultValue = "10") long size
    ) {
        return ApiResponse.success(washOrderService.getSimpleOrderList(userId, size));
    }

    @PostMapping("/simple-create")
    public ApiResponse<WashOrder> simpleCreate(@RequestBody SimpleOrderCreateRequest request) {
        return ApiResponse.success("created", washOrderService.createSimpleOrder(request));
    }

    @PostMapping("/{id}/start")
    public ApiResponse<WashOrder> start(@PathVariable Long id) {
        return ApiResponse.success("started", washOrderService.startOrder(id));
    }

    @PostMapping("/{id}/complete")
    public ApiResponse<WashOrder> complete(@PathVariable Long id) {
        return ApiResponse.success("completed", washOrderService.completeOrder(id));
    }

    @GetMapping("/{id}")
    public ApiResponse<WashOrder> getById(@PathVariable Long id) {
        WashOrder order = washOrderService.getById(id);
        if (order == null) {
            throw new IllegalArgumentException("order not found");
        }
        return ApiResponse.success(order);
    }

    @GetMapping("/{id}/status-logs")
    public ApiResponse<List<WashOrderStatusLog>> getStatusLogs(@PathVariable Long id) {
        return ApiResponse.success(washOrderService.getStatusLogs(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<WashOrder> update(@PathVariable Long id, @RequestBody WashOrder order) {
        order.setId(id);
        boolean updated = washOrderService.updateById(order);
        if (!updated) {
            throw new IllegalArgumentException("order update failed");
        }
        return ApiResponse.success("updated", washOrderService.getById(id));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Boolean> delete(@PathVariable Long id) {
        boolean removed = washOrderService.removeById(id);
        if (!removed) {
            throw new IllegalArgumentException("order delete failed");
        }
        return ApiResponse.success("deleted", true);
    }
}
