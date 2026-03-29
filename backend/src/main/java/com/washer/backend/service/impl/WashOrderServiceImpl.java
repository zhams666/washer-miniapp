package com.washer.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.washer.backend.dto.order.SimpleOrderCreateRequest;
import com.washer.backend.dto.order.SimpleOrderItem;
import com.washer.backend.entity.Store;
import com.washer.backend.entity.WashOrder;
import com.washer.backend.entity.WashOrderStatusLog;
import com.washer.backend.mapper.WashOrderMapper;
import com.washer.backend.mapper.WashOrderStatusLogMapper;
import com.washer.backend.service.StoreService;
import com.washer.backend.service.WashOrderService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class WashOrderServiceImpl extends ServiceImpl<WashOrderMapper, WashOrder> implements WashOrderService {

    private static final String STATUS_PENDING = "pending";
    private static final String STATUS_RUNNING = "running";
    private static final String STATUS_COMPLETED = "completed";

    private final StoreService storeService;
    private final WashOrderStatusLogMapper washOrderStatusLogMapper;

    public WashOrderServiceImpl(StoreService storeService, WashOrderStatusLogMapper washOrderStatusLogMapper) {
        this.storeService = storeService;
        this.washOrderStatusLogMapper = washOrderStatusLogMapper;
    }

    @Override
    public List<SimpleOrderItem> getSimpleOrderList(Long userId, long size) {
        LambdaQueryWrapper<WashOrder> wrapper = new LambdaQueryWrapper<WashOrder>()
            .eq(userId != null, WashOrder::getUserId, userId)
            .orderByDesc(WashOrder::getId)
            .last("limit " + size);

        List<WashOrder> orders = this.list(wrapper);
        List<Long> storeIds = orders.stream()
            .map(WashOrder::getStoreId)
            .filter(id -> id != null)
            .distinct()
            .toList();

        Map<Long, Store> storeMap = storeIds.isEmpty()
            ? Map.of()
            : storeService.listByIds(storeIds).stream()
                .collect(Collectors.toMap(Store::getId, Function.identity(), (a, b) -> a));

        return orders.stream()
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
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WashOrder createSimpleOrder(SimpleOrderCreateRequest request) {
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
        order.setOrderStatus(STATUS_PENDING);
        order.setPayMode("wallet");
        order.setPaymentStatus("unpaid");
        order.setRefundStatusSnapshot("none");
        order.setPricingSnapshotVersion(1);
        order.setCardDeductTimes(0);
        order.setEstimatedAmount(request.getFinalAmount() != null ? request.getFinalAmount() : BigDecimal.ZERO);
        order.setFinalAmount(request.getFinalAmount() != null ? request.getFinalAmount() : BigDecimal.ZERO);
        order.setPaidAmount(BigDecimal.ZERO);
        order.setRefundAmount(BigDecimal.ZERO);
        order.setIsFirstPeriodDiscountUsed(0);
        order.setFirstPeriodDiscountAmount(BigDecimal.ZERO);
        order.setRemark(StringUtils.hasText(request.getRemark()) ? request.getRemark() : "simple test order");
        this.save(order);

        insertStatusLog(order, null, STATUS_PENDING, "create", "user", request.getUserId(), order.getRemark());
        return order;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WashOrder startOrder(Long id) {
        WashOrder order = getRequiredOrder(id);
        if (STATUS_RUNNING.equals(order.getOrderStatus())) {
            return order;
        }
        if (STATUS_COMPLETED.equals(order.getOrderStatus())) {
            throw new IllegalArgumentException("completed order cannot be started");
        }

        String fromStatus = order.getOrderStatus();
        LambdaUpdateWrapper<WashOrder> wrapper = new LambdaUpdateWrapper<WashOrder>()
            .eq(WashOrder::getId, id)
            .set(WashOrder::getOrderStatus, STATUS_RUNNING)
            .set(WashOrder::getStartTime, order.getStartTime() != null ? order.getStartTime() : LocalDateTime.now())
            .set(WashOrder::getPaymentStatus, "unpaid");
        this.update(wrapper);

        WashOrder updatedOrder = getRequiredOrder(id);
        insertStatusLog(updatedOrder, fromStatus, STATUS_RUNNING, "start", "user", updatedOrder.getUserId(), "start order");
        return updatedOrder;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WashOrder completeOrder(Long id) {
        WashOrder order = getRequiredOrder(id);
        if (STATUS_COMPLETED.equals(order.getOrderStatus())) {
            return order;
        }

        String fromStatus = order.getOrderStatus();
        LambdaUpdateWrapper<WashOrder> wrapper = new LambdaUpdateWrapper<WashOrder>()
            .eq(WashOrder::getId, id)
            .set(WashOrder::getOrderStatus, STATUS_COMPLETED)
            .set(WashOrder::getEndTime, LocalDateTime.now())
            .set(WashOrder::getSettleTime, LocalDateTime.now())
            .set(WashOrder::getPaymentStatus, "paid")
            .set(WashOrder::getPaidAmount, order.getFinalAmount() != null ? order.getFinalAmount() : BigDecimal.ZERO);
        this.update(wrapper);

        WashOrder updatedOrder = getRequiredOrder(id);
        insertStatusLog(updatedOrder, fromStatus, STATUS_COMPLETED, "finish", "user", updatedOrder.getUserId(), "complete order");
        return updatedOrder;
    }

    @Override
    public List<WashOrderStatusLog> getStatusLogs(Long orderId) {
        return washOrderStatusLogMapper.selectList(
            new LambdaQueryWrapper<WashOrderStatusLog>()
                .eq(WashOrderStatusLog::getOrderId, orderId)
                .orderByAsc(WashOrderStatusLog::getId)
        );
    }

    private WashOrder getRequiredOrder(Long id) {
        WashOrder order = this.getById(id);
        if (order == null) {
            throw new IllegalArgumentException("order not found");
        }
        return order;
    }

    private void insertStatusLog(
        WashOrder order,
        String fromStatus,
        String toStatus,
        String actionType,
        String operatorType,
        Long operatorId,
        String remark
    ) {
        WashOrderStatusLog log = new WashOrderStatusLog();
        log.setOrderId(order.getId());
        log.setOrderNo(order.getOrderNo());
        log.setFromStatus(fromStatus);
        log.setToStatus(toStatus);
        log.setActionType(actionType);
        log.setOperatorType(operatorType);
        log.setOperatorId(operatorId);
        log.setRemark(remark);
        washOrderStatusLogMapper.insert(log);
    }
}
