package com.washer.backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.washer.backend.common.ApiResponse;
import com.washer.backend.dto.order.SimpleOrderCreateRequest;
import com.washer.backend.dto.order.SimpleOrderItem;
import com.washer.backend.entity.Store;
import com.washer.backend.entity.WashOrder;
import com.washer.backend.service.StoreService;
import com.washer.backend.service.WashOrderService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
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
@RequestMapping("/api/orders")
public class WashOrderController {

    private final WashOrderService washOrderService;
    private final StoreService storeService;

    public WashOrderController(WashOrderService washOrderService, StoreService storeService) {
        this.washOrderService = washOrderService;
        this.storeService = storeService;
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
        LambdaQueryWrapper<WashOrder> wrapper = new LambdaQueryWrapper<WashOrder>()
            .eq(userId != null, WashOrder::getUserId, userId)
            .eq(storeId != null, WashOrder::getStoreId, storeId)
            .eq(StringUtils.hasText(orderStatus), WashOrder::getOrderStatus, orderStatus)
            .orderByDesc(WashOrder::getId);

        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w
                .like(WashOrder::getOrderNo, keyword)
                .or()
                .like(WashOrder::getRemark, keyword));
        }

        return ApiResponse.success(washOrderService.page(new Page<>(page, size), wrapper));
    }

    @GetMapping("/simple-list")
    public ApiResponse<List<SimpleOrderItem>> simpleList(
        @RequestParam(required = false) Long userId,
        @RequestParam(defaultValue = "10") long size
    ) {
        LambdaQueryWrapper<WashOrder> wrapper = new LambdaQueryWrapper<WashOrder>()
            .eq(userId != null, WashOrder::getUserId, userId)
            .orderByDesc(WashOrder::getId)
            .last("limit " + size);

        List<WashOrder> orders = washOrderService.list(wrapper);
        List<Long> storeIds = orders.stream()
            .map(WashOrder::getStoreId)
            .filter(id -> id != null)
            .distinct()
            .toList();

        Map<Long, Store> storeMap = storeIds.isEmpty()
            ? Map.of()
            : storeService.listByIds(storeIds).stream()
                .collect(Collectors.toMap(Store::getId, Function.identity(), (a, b) -> a));

        List<SimpleOrderItem> result = orders.stream()
            .map(order -> {
                Store store = storeMap.get(order.getStoreId());
                return new SimpleOrderItem(
                    order.getId(),
                    order.getOrderNo(),
                    order.getUserId(),
                    order.getStoreId(),
                    store != null ? store.getStoreName() : "Unknown Store",
                    order.getOrderStatus(),
                    order.getPaymentStatus(),
                    order.getFinalAmount(),
                    order.getCreatedAt(),
                    order.getRemark()
                );
            })
            .toList();

        return ApiResponse.success(result);
    }

    @PostMapping("/simple-create")
    public ApiResponse<WashOrder> simpleCreate(@RequestBody SimpleOrderCreateRequest request) {
        if (request.getUserId() == null) {
            throw new IllegalArgumentException("userId is required");
        }
        if (request.getStoreId() == null) {
            throw new IllegalArgumentException("storeId is required");
        }

        WashOrder order = new WashOrder();
        order.setOrderNo("WO" + UUID.randomUUID().toString().replace("-", "").substring(0, 18));
        order.setUserId(request.getUserId());
        order.setStoreId(request.getStoreId());
        order.setDeviceId(request.getDeviceId());
        order.setOrderSource("miniapp");
        order.setOrderStatus("completed");
        order.setPayMode("wallet");
        order.setPaymentStatus("paid");
        order.setRefundStatusSnapshot("none");
        order.setPricingSnapshotVersion(1);
        order.setCardDeductTimes(0);
        order.setEstimatedAmount(request.getFinalAmount() != null ? request.getFinalAmount() : BigDecimal.ZERO);
        order.setFinalAmount(request.getFinalAmount() != null ? request.getFinalAmount() : BigDecimal.ZERO);
        order.setPaidAmount(request.getFinalAmount() != null ? request.getFinalAmount() : BigDecimal.ZERO);
        order.setRefundAmount(BigDecimal.ZERO);
        order.setIsFirstPeriodDiscountUsed(0);
        order.setFirstPeriodDiscountAmount(BigDecimal.ZERO);
        order.setRemark(StringUtils.hasText(request.getRemark()) ? request.getRemark() : "simple test order");
        washOrderService.save(order);
        return ApiResponse.success("created", order);
    }

    @GetMapping("/{id}")
    public ApiResponse<WashOrder> getById(@PathVariable Long id) {
        WashOrder order = washOrderService.getById(id);
        if (order == null) {
            throw new IllegalArgumentException("order not found");
        }
        return ApiResponse.success(order);
    }

    @PostMapping
    public ApiResponse<WashOrder> create(@RequestBody WashOrder order) {
        if (order.getUserId() == null) {
            throw new IllegalArgumentException("userId is required");
        }
        if (order.getStoreId() == null) {
            throw new IllegalArgumentException("storeId is required");
        }
        if (!StringUtils.hasText(order.getOrderNo())) {
            order.setOrderNo("WO" + UUID.randomUUID().toString().replace("-", "").substring(0, 18));
        }
        if (!StringUtils.hasText(order.getOrderSource())) {
            order.setOrderSource("admin");
        }
        if (!StringUtils.hasText(order.getOrderStatus())) {
            order.setOrderStatus("pending");
        }
        if (!StringUtils.hasText(order.getPayMode())) {
            order.setPayMode("wallet");
        }
        if (!StringUtils.hasText(order.getPaymentStatus())) {
            order.setPaymentStatus("unpaid");
        }
        if (!StringUtils.hasText(order.getRefundStatusSnapshot())) {
            order.setRefundStatusSnapshot("none");
        }
        if (order.getPricingSnapshotVersion() == null) {
            order.setPricingSnapshotVersion(1);
        }
        if (order.getCardDeductTimes() == null) {
            order.setCardDeductTimes(0);
        }
        if (order.getEstimatedAmount() == null) {
            order.setEstimatedAmount(BigDecimal.ZERO);
        }
        if (order.getFinalAmount() == null) {
            order.setFinalAmount(BigDecimal.ZERO);
        }
        if (order.getPaidAmount() == null) {
            order.setPaidAmount(BigDecimal.ZERO);
        }
        if (order.getRefundAmount() == null) {
            order.setRefundAmount(BigDecimal.ZERO);
        }
        if (order.getIsFirstPeriodDiscountUsed() == null) {
            order.setIsFirstPeriodDiscountUsed(0);
        }
        if (order.getFirstPeriodDiscountAmount() == null) {
            order.setFirstPeriodDiscountAmount(BigDecimal.ZERO);
        }
        washOrderService.save(order);
        return ApiResponse.success("created", order);
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
