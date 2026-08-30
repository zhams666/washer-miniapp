package com.washer.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.washer.backend.dto.admin.AdminOrderDetail;
import com.washer.backend.dto.admin.AdminOrderListItem;
import com.washer.backend.dto.order.SimpleOrderCreateRequest;
import com.washer.backend.dto.order.SimpleOrderItem;
import com.washer.backend.dto.pricing.WashPricingSnapshot;
import com.washer.backend.entity.CardUsageRecord;
import com.washer.backend.entity.Device;
import com.washer.backend.entity.Store;
import com.washer.backend.entity.StoreSettlementDetail;
import com.washer.backend.entity.UserCard;
import com.washer.backend.entity.UserDailyDiscountRecord;
import com.washer.backend.entity.UserInfo;
import com.washer.backend.entity.UserStoreWallet;
import com.washer.backend.entity.WashOrder;
import com.washer.backend.entity.WashOrderPaymentDetail;
import com.washer.backend.entity.WashOrderStatusLog;
import com.washer.backend.entity.WalletTransaction;
import com.washer.backend.mapper.CardUsageRecordMapper;
import com.washer.backend.mapper.StoreSettlementDetailMapper;
import com.washer.backend.mapper.UserCardMapper;
import com.washer.backend.mapper.UserDailyDiscountRecordMapper;
import com.washer.backend.mapper.UserStoreWalletMapper;
import com.washer.backend.mapper.WashOrderMapper;
import com.washer.backend.mapper.WashOrderPaymentDetailMapper;
import com.washer.backend.mapper.WashOrderStatusLogMapper;
import com.washer.backend.mapper.WalletTransactionMapper;
import com.washer.backend.service.DeviceService;
import com.washer.backend.service.StoreService;
import com.washer.backend.service.UserInfoService;
import com.washer.backend.service.WashPricingService;
import com.washer.backend.service.WashOrderService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class WashOrderServiceImpl extends ServiceImpl<WashOrderMapper, WashOrder> implements WashOrderService {

    private static final String STATUS_PENDING = "pending";
    private static final String STATUS_RUNNING = "running";
    private static final String STATUS_COMPLETED = "completed";
    private static final String STATUS_CANCELLED = "cancelled";
    private static final String STATUS_CLOSED = "closed";
    private static final String STATUS_ABNORMAL = "abnormal";
    private static final String PAY_MODE_WALLET = "wallet";
    private static final String PAY_MODE_CARD = "card";
    private static final String MONTHLY_CARD_TYPE = "monthly";
    private static final String DISCOUNT_TYPE_FIRST_PERIOD = "first_period_discount";
    private static final String DISCOUNT_SCOPE_USER_STORE_DAY = "user_store_day";
    private static final BigDecimal DEFAULT_FIRST_PERIOD_DISCOUNT_AMOUNT = new BigDecimal("5.00");
    private static final String AUTO_STOP_BALANCE_NOT_ENOUGH = "余额不足自动停止";
    private static final int CARD_ORDER_LIMIT_MINUTES = 30;
    private static final String CARD_AUTO_STOP_REMARK = "次卡30分钟到期自动停止";

    private final StoreService storeService;
    private final DeviceService deviceService;
    private final WashOrderStatusLogMapper washOrderStatusLogMapper;
    private final UserCardMapper userCardMapper;
    private final CardUsageRecordMapper cardUsageRecordMapper;
    private final UserDailyDiscountRecordMapper userDailyDiscountRecordMapper;
    private final UserStoreWalletMapper userStoreWalletMapper;
    private final WalletTransactionMapper walletTransactionMapper;
    private final WashOrderPaymentDetailMapper washOrderPaymentDetailMapper;
    private final StoreSettlementDetailMapper storeSettlementDetailMapper;
    private final WashPricingService washPricingService;
    private final UserInfoService userInfoService;

    public WashOrderServiceImpl(
        StoreService storeService,
        DeviceService deviceService,
        WashOrderStatusLogMapper washOrderStatusLogMapper,
        UserCardMapper userCardMapper,
        CardUsageRecordMapper cardUsageRecordMapper,
        UserDailyDiscountRecordMapper userDailyDiscountRecordMapper,
        UserStoreWalletMapper userStoreWalletMapper,
        WalletTransactionMapper walletTransactionMapper,
        WashOrderPaymentDetailMapper washOrderPaymentDetailMapper,
        StoreSettlementDetailMapper storeSettlementDetailMapper,
        WashPricingService washPricingService,
        UserInfoService userInfoService
    ) {
        this.storeService = storeService;
        this.deviceService = deviceService;
        this.washOrderStatusLogMapper = washOrderStatusLogMapper;
        this.userCardMapper = userCardMapper;
        this.cardUsageRecordMapper = cardUsageRecordMapper;
        this.userDailyDiscountRecordMapper = userDailyDiscountRecordMapper;
        this.userStoreWalletMapper = userStoreWalletMapper;
        this.walletTransactionMapper = walletTransactionMapper;
        this.washOrderPaymentDetailMapper = washOrderPaymentDetailMapper;
        this.storeSettlementDetailMapper = storeSettlementDetailMapper;
        this.washPricingService = washPricingService;
        this.userInfoService = userInfoService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<SimpleOrderItem> getSimpleOrderList(Long userId, long size) {
        LambdaQueryWrapper<WashOrder> wrapper = new LambdaQueryWrapper<WashOrder>()
            .eq(userId != null, WashOrder::getUserId, userId)
            .orderByDesc(WashOrder::getId)
            .last("limit " + size);

        List<WashOrder> orders = this.list(wrapper);
        orders = orders.stream()
            .map(this::refreshRunningOrderForDisplay)
            .toList();
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
                    order.getEstimatedAmount(),
                    resolveSimpleListAmount(order),
                    order.getCreatedAt(),
                    order.getRemark(),
                    order.getPayMode(),
                    order.getCardDeductTimes(),
                    CARD_ORDER_LIMIT_MINUTES
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
        normalizeOrderTarget(request);

        WashPricingSnapshot pricingSnapshot = resolvePricingSnapshotForStart(
            request.getUserId(),
            request.getStoreId(),
            LocalDateTime.now()
        );
        BigDecimal baseAmount = washPricingService.getBasePrice(pricingSnapshot);
        String payMode = resolvePayModeForCreate(request.getUserId(), request.getStoreId(), request.getPayMode());
        if (PAY_MODE_WALLET.equals(payMode)) {
            ensureWalletCanCoverBaseAmount(request.getUserId(), request.getStoreId(), baseAmount);
        }
        BigDecimal orderAmount = PAY_MODE_CARD.equals(payMode) ? BigDecimal.ZERO : baseAmount;

        WashOrder order = new WashOrder();
        order.setOrderNo("WO" + UUID.randomUUID().toString().replace("-", "").substring(0, 18));
        order.setUserId(request.getUserId());
        order.setStoreId(request.getStoreId());
        order.setBayId(request.getBayId());
        order.setDeviceId(request.getDeviceId());
        order.setOrderSource("miniapp");
        order.setOrderStatus(STATUS_PENDING);
        order.setPayMode(payMode);
        order.setPaymentStatus("unpaid");
        order.setRefundStatusSnapshot("none");
        order.setPricingRuleId(pricingSnapshot.pricingRuleId());
        order.setPricingSnapshot(washPricingService.serializeSnapshot(pricingSnapshot));
        order.setPricingSnapshotVersion(pricingSnapshot.ruleVersion());
        order.setCardDeductTimes(0);
        order.setEstimatedAmount(orderAmount);
        order.setFinalAmount(orderAmount);
        order.setPaidAmount(BigDecimal.ZERO);
        order.setRefundAmount(BigDecimal.ZERO);
        order.setIsFirstPeriodDiscountUsed(0);
        order.setFirstPeriodDiscountAmount(BigDecimal.ZERO);
        order.setRemark(StringUtils.hasText(request.getRemark()) ? request.getRemark() : "simple test order");
        this.save(order);

        insertStatusLog(order, null, STATUS_PENDING, "create", "user", request.getUserId(), "创建订单");
        return order;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WashOrder createAndStartWashOrder(SimpleOrderCreateRequest request) {
        WashOrder order = createSimpleOrder(request);
        return startOrder(order.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WashOrder startOrder(Long id) {
        WashOrder order = getRequiredOrderForUpdate(id);
        if (STATUS_RUNNING.equals(order.getOrderStatus())) {
            return order;
        }
        ensureOrderCanStart(order);

        Device device = lockRequiredDeviceForStart(order);
        ensureBayIsIdle(order);

        String fromStatus = order.getOrderStatus();
        LocalDateTime startTime = order.getStartTime() != null ? order.getStartTime() : LocalDateTime.now();
        WashPricingSnapshot pricingSnapshot = resolvePricingSnapshotForStart(order.getUserId(), order.getStoreId(), startTime);
        BigDecimal startAmount = washPricingService.calculateAmount(pricingSnapshot, startTime, startTime);
        BigDecimal orderAmount = PAY_MODE_CARD.equals(order.getPayMode()) ? BigDecimal.ZERO : startAmount;
        UserCard lockedCard = null;
        if (PAY_MODE_WALLET.equals(order.getPayMode())) {
            ensureWalletCanCoverBaseAmount(order.getUserId(), order.getStoreId(), startAmount);
        } else if (PAY_MODE_CARD.equals(order.getPayMode())) {
            lockedCard = lockAvailableCardForOrder(order);
        } else {
            throw new IllegalArgumentException("unsupported pay_mode");
        }
        LambdaUpdateWrapper<WashOrder> wrapper = new LambdaUpdateWrapper<WashOrder>()
            .eq(WashOrder::getId, order.getId())
            .eq(WashOrder::getOrderStatus, STATUS_PENDING)
            .set(WashOrder::getOrderStatus, STATUS_RUNNING)
            .set(WashOrder::getStartTime, startTime)
            .set(WashOrder::getPaymentStatus, "unpaid")
            .set(WashOrder::getDeviceStatusSnapshot, device.getDeviceStatus())
            .set(WashOrder::getStartCommandNo, buildCommandNo("START", order))
            .set(WashOrder::getIsFirstPeriodDiscountUsed, 0)
            .set(WashOrder::getFirstPeriodDiscountAmount, BigDecimal.ZERO)
            .set(WashOrder::getEstimatedAmount, orderAmount)
            .set(WashOrder::getFinalAmount, orderAmount)
            .set(WashOrder::getPricingRuleId, pricingSnapshot.pricingRuleId())
            .set(WashOrder::getPricingSnapshot, washPricingService.serializeSnapshot(pricingSnapshot))
            .set(WashOrder::getPricingSnapshotVersion, pricingSnapshot.ruleVersion());
        if (lockedCard != null) {
            wrapper
                .set(WashOrder::getCardUsageId, lockedCard.getId())
                .set(WashOrder::getCardDeductTimes, 1);
        }
        boolean updated = this.update(wrapper);
        if (!updated) {
            WashOrder latest = getRequiredOrderForUpdate(id);
            if (STATUS_RUNNING.equals(latest.getOrderStatus())) {
                return latest;
            }
            throw new IllegalStateException("order status changed while starting");
        }
        markDeviceStatus(device, STATUS_RUNNING);

        WashOrder updatedOrder = getRequiredOrder(id);
        insertStatusLog(updatedOrder, fromStatus, STATUS_RUNNING, "start", "user", updatedOrder.getUserId(), "开始洗车，按时长计费");
        return updatedOrder;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WashOrder completeOrder(Long id) {
        WashOrder order = getRequiredOrderForUpdate(id);
        if (STATUS_COMPLETED.equals(order.getOrderStatus())) {
            return order;
        }
        ensureOrderCanComplete(order);
        lockDeviceForOrder(order);

        if (PAY_MODE_WALLET.equals(order.getPayMode())) {
            return completeWalletOrder(order);
        }

        if (PAY_MODE_CARD.equals(order.getPayMode())) {
            return completeCardOrder(order);
        }

        throw new IllegalArgumentException("unsupported pay_mode");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WashOrder cancelOrder(Long id) {
        return cancelOrder(id, "取消订单");
    }

    private WashOrder cancelOrder(Long id, String cancelRemark) {
        WashOrder order = getRequiredOrderForUpdate(id);
        if (STATUS_CANCELLED.equals(order.getOrderStatus())) {
            return order;
        }
        if (STATUS_COMPLETED.equals(order.getOrderStatus())) {
            return order;
        }
        if (STATUS_CLOSED.equals(order.getOrderStatus())) {
            return order;
        }

        String fromStatus = order.getOrderStatus();
        Device device = null;
        if (STATUS_RUNNING.equals(fromStatus)) {
            device = lockDeviceForOrder(order);
        }

        LocalDateTime cancelTime = LocalDateTime.now();
        LambdaUpdateWrapper<WashOrder> wrapper = new LambdaUpdateWrapper<WashOrder>()
            .eq(WashOrder::getId, order.getId())
            .ne(WashOrder::getOrderStatus, STATUS_COMPLETED)
            .set(WashOrder::getOrderStatus, STATUS_CANCELLED)
            .set(WashOrder::getCancelTime, cancelTime)
            .set(WashOrder::getPaymentStatus, "unpaid")
            .set(WashOrder::getCancelCommandNo, buildCommandNo("CANCEL", order));
        if (PAY_MODE_CARD.equals(order.getPayMode())) {
            releaseLockedCard(order);
            wrapper
                .set(WashOrder::getCardUsageId, null)
                .set(WashOrder::getCardDeductTimes, 0);
        }
        this.update(wrapper);

        if (device != null) {
            markDeviceStatus(device, "idle");
        }

        WashOrder updatedOrder = getRequiredOrder(order.getId());
        insertStatusLog(
            updatedOrder,
            fromStatus,
            STATUS_CANCELLED,
            "cancel",
            "user",
            updatedOrder.getUserId(),
            StringUtils.hasText(cancelRemark) ? cancelRemark : "取消订单"
        );
        return updatedOrder;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int cancelRunningOrdersForDevice(Long deviceId, String remark) {
        if (deviceId == null) {
            return 0;
        }

        Device device = deviceService.lambdaQuery()
            .eq(Device::getId, deviceId)
            .last("limit 1 for update")
            .one();
        if (device == null || device.getStoreId() == null) {
            return 0;
        }

        Long bayId = resolveBayId(device);
        LambdaQueryWrapper<WashOrder> wrapper = new LambdaQueryWrapper<WashOrder>()
            .eq(WashOrder::getStoreId, device.getStoreId())
            .eq(WashOrder::getOrderStatus, STATUS_RUNNING);
        wrapper.and(w -> {
            w.eq(WashOrder::getDeviceId, deviceId);
            if (bayId != null) {
                w.or().eq(WashOrder::getBayId, bayId);
            }
        });

        List<WashOrder> runningOrders = this.list(wrapper);
        for (WashOrder order : runningOrders) {
            cancelOrder(order.getId(), remark);
        }
        return runningOrders.size();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WashOrder checkAndAutoStopOrder(Long id) {
        WashOrder order = getRequiredOrder(id);
        if (!STATUS_RUNNING.equals(order.getOrderStatus())) {
            return order;
        }
        if (!PAY_MODE_WALLET.equals(order.getPayMode())) {
            if (PAY_MODE_CARD.equals(order.getPayMode())) {
                if (shouldAutoStopCardOrder(order, LocalDateTime.now())) {
                    return completeCardOrder(order, CARD_AUTO_STOP_REMARK);
                }
                order.setEstimatedAmount(BigDecimal.ZERO);
                order.setFinalAmount(BigDecimal.ZERO);
                order.setPaidAmount(BigDecimal.ZERO);
            }
            return order;
        }

        BigDecimal currentAmount = calculateWashOrderAmount(order, LocalDateTime.now());
        BigDecimal walletAvailable = calculateWalletAvailableForOrder(order);
        if (walletAvailable.compareTo(BigDecimal.ZERO) <= 0 || currentAmount.compareTo(walletAvailable) >= 0) {
            return completeWalletOrder(order, true, AUTO_STOP_BALANCE_NOT_ENOUGH);
        }
        order.setEstimatedAmount(currentAmount);
        order.setFinalAmount(currentAmount);
        return order;
    }

    @Scheduled(fixedDelay = 30000)
    @Transactional(rollbackFor = Exception.class)
    public void autoCompleteExpiredCardOrders() {
        LocalDateTime now = LocalDateTime.now();
        List<WashOrder> expiredOrders = this.list(
            new LambdaQueryWrapper<WashOrder>()
                .eq(WashOrder::getOrderStatus, STATUS_RUNNING)
                .eq(WashOrder::getPayMode, PAY_MODE_CARD)
                .le(WashOrder::getStartTime, now.minusMinutes(CARD_ORDER_LIMIT_MINUTES))
                .orderByAsc(WashOrder::getId)
                .last("limit 20")
        );
        for (WashOrder order : expiredOrders) {
            WashOrder lockedOrder = getRequiredOrderForUpdate(order.getId());
            if (STATUS_RUNNING.equals(lockedOrder.getOrderStatus())
                && shouldAutoStopCardOrder(lockedOrder, now)) {
                completeCardOrder(lockedOrder, CARD_AUTO_STOP_REMARK);
            }
        }
    }

    private WashOrder completeWalletOrder(WashOrder order) {
        return completeWalletOrder(order, false, "finish order");
    }

    private WashOrder completeWalletOrder(WashOrder order, boolean capByWalletBalance, String finishRemark) {
        String fromStatus = order.getOrderStatus();
        LocalDateTime finishTime = LocalDateTime.now();
        BigDecimal calculatedAmount = calculateWashOrderAmount(order, finishTime);
        BigDecimal walletAvailable = calculateWalletAvailableForOrder(order);
        boolean shouldCapByBalance = capByWalletBalance
            || walletAvailable.compareTo(BigDecimal.ZERO) <= 0
            || calculatedAmount.compareTo(walletAvailable) >= 0;
        if (shouldCapByBalance) {
            finishRemark = AUTO_STOP_BALANCE_NOT_ENOUGH;
        }
        BigDecimal chargeAmount = shouldCapByBalance ? calculatedAmount.min(walletAvailable) : calculatedAmount;
        chargeAmount = normalizeAmount(chargeAmount).setScale(2, RoundingMode.HALF_UP);

        WalletAllocation allocation = buildWalletAllocation(order, chargeAmount);
        if (allocation.totalAmount().compareTo(BigDecimal.ZERO) > 0) {
            String bizActionNo = "ORDER_PAY_" + order.getOrderNo();
            int paymentSeq = 1;
            for (WalletDeduction deduction : allocation.deductions()) {
                applyWalletDeduction(order, deduction, bizActionNo, paymentSeq);
                paymentSeq++;
            }
        }

        String paymentStatusDesc = allocation.totalAmount().compareTo(BigDecimal.ZERO) <= 0
            ? "wallet paid"
            : (allocation.hasGift() ? "wallet principal + gift paid" : "wallet principal paid");
        String abnormalReason = shouldCapByBalance ? AUTO_STOP_BALANCE_NOT_ENOUGH : null;
        updateOrderAsPaid(order, chargeAmount, allocation.totalAmount(), paymentStatusDesc, null, 0, finishTime, abnormalReason);
        generateSettlementDetails(order);

        WashOrder updatedOrder = getRequiredOrder(order.getId());
        insertStatusLog(updatedOrder, fromStatus, STATUS_COMPLETED, "finish", "user", updatedOrder.getUserId(), finishRemark);
        return updatedOrder;
    }

    private WashOrder completeCardOrder(WashOrder order) {
        return completeCardOrder(order, "完成次卡订单");
    }

    private WashOrder completeCardOrder(WashOrder order, String finishRemark) {
        String fromStatus = order.getOrderStatus();
        BigDecimal finalAmount = BigDecimal.ZERO;
        UserCard userCard = getRequiredLockedCard(order);
        Integer remainingTimes = userCard.getRemainingTimes() != null ? userCard.getRemainingTimes() : 0;
        if (remainingTimes < 1) {
            throw new IllegalArgumentException("user card remaining times is not enough");
        }

        Integer usedTimes = userCard.getUsedTimes() != null ? userCard.getUsedTimes() : 0;
        Integer newRemainingTimes = remainingTimes - 1;
        String newStatus = newRemainingTimes > 0 ? "active" : "used_up";

        LambdaUpdateWrapper<UserCard> cardWrapper = new LambdaUpdateWrapper<UserCard>()
            .eq(UserCard::getId, userCard.getId())
            .eq(UserCard::getStatus, "locked")
            .gt(UserCard::getRemainingTimes, 0)
            .set(UserCard::getUsedTimes, usedTimes + 1)
            .set(UserCard::getRemainingTimes, newRemainingTimes)
            .set(UserCard::getStatus, newStatus);
        int updatedCardRows = userCardMapper.update(null, cardWrapper);
        if (updatedCardRows <= 0) {
            throw new IllegalStateException("locked user card changed while completing order");
        }

        CardUsageRecord usageRecord = insertCardUsageRecord(order, userCard);
        String bizActionNo = "ORDER_PAY_" + order.getOrderNo();
        insertCardPaymentDetail(order, finalAmount, bizActionNo, userCard.getId());

        updateOrderAsPaid(order, finalAmount, BigDecimal.ZERO, "card paid", usageRecord.getId(), 1, LocalDateTime.now(), null);

        WashOrder updatedOrder = getRequiredOrder(order.getId());
        insertStatusLog(updatedOrder, fromStatus, STATUS_COMPLETED, "finish", "user", updatedOrder.getUserId(), finishRemark);
        return updatedOrder;
    }

    private void updateOrderAsPaid(
        WashOrder order,
        BigDecimal finalAmount,
        BigDecimal paidAmount,
        String paymentStatusDesc,
        Long cardUsageId,
        Integer cardDeductTimes,
        LocalDateTime settleTime,
        String abnormalReason
    ) {
        order.setOrderStatus(STATUS_COMPLETED);
        order.setEndTime(settleTime);
        order.setSettleTime(settleTime);
        order.setPaymentStatus("paid");
        order.setPaymentStatusDesc(paymentStatusDesc);
        order.setEstimatedAmount(finalAmount);
        order.setFinalAmount(finalAmount);
        order.setPaidAmount(paidAmount);
        order.setAbnormalReason(abnormalReason);
        LambdaUpdateWrapper<WashOrder> wrapper = new LambdaUpdateWrapper<WashOrder>()
            .eq(WashOrder::getId, order.getId())
            .set(WashOrder::getOrderStatus, STATUS_COMPLETED)
            .set(WashOrder::getEndTime, settleTime)
            .set(WashOrder::getSettleTime, settleTime)
            .set(WashOrder::getPaymentStatus, "paid")
            .set(WashOrder::getPaymentStatusDesc, paymentStatusDesc)
            .set(WashOrder::getEstimatedAmount, finalAmount)
            .set(WashOrder::getFinalAmount, finalAmount)
            .set(WashOrder::getPaidAmount, paidAmount)
            .set(WashOrder::getAbnormalReason, abnormalReason);
        if (cardUsageId != null) {
            wrapper.set(WashOrder::getCardUsageId, cardUsageId);
        }
        wrapper.set(WashOrder::getCardDeductTimes, cardDeductTimes);
        this.update(wrapper);
        markOrderDeviceIdle(order);
    }

    @Override
    public List<WashOrderStatusLog> getStatusLogs(Long orderId) {
        return washOrderStatusLogMapper.selectList(
            new LambdaQueryWrapper<WashOrderStatusLog>()
                .eq(WashOrderStatusLog::getOrderId, orderId)
                .orderByAsc(WashOrderStatusLog::getId)
        );
    }

    @Override
    public List<WashOrderPaymentDetail> getPaymentDetails(Long orderId) {
        return washOrderPaymentDetailMapper.selectList(
            new LambdaQueryWrapper<WashOrderPaymentDetail>()
                .eq(WashOrderPaymentDetail::getOrderId, orderId)
                .orderByAsc(WashOrderPaymentDetail::getId)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getDurationRanking(String scope, Long userId, int limit) {
        String resolvedScope = resolveRankingScope(scope);
        int rowLimit = Math.max(1, Math.min(limit, 50));
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime fromTime = resolveRankingStartTime(resolvedScope, now);

        LambdaQueryWrapper<WashOrder> wrapper = new LambdaQueryWrapper<WashOrder>()
            .eq(WashOrder::getOrderStatus, STATUS_COMPLETED)
            .isNotNull(WashOrder::getUserId)
            .isNotNull(WashOrder::getStartTime)
            .isNotNull(WashOrder::getEndTime)
            .ge(fromTime != null, WashOrder::getEndTime, fromTime)
            .le(WashOrder::getEndTime, now)
            .orderByDesc(WashOrder::getEndTime);

        List<WashOrder> orders = this.list(wrapper);
        Map<Long, DurationRankAggregate> aggregateMap = new HashMap<>();
        for (WashOrder order : orders) {
            Long orderUserId = order.getUserId();
            long seconds = calculateOrderDurationSeconds(order);
            if (orderUserId == null || seconds <= 0) {
                continue;
            }

            DurationRankAggregate aggregate = aggregateMap.computeIfAbsent(
                orderUserId,
                DurationRankAggregate::new
            );
            aggregate.totalSeconds += seconds;
            aggregate.orderCount += 1;
            if (aggregate.latestEndTime == null || order.getEndTime().isAfter(aggregate.latestEndTime)) {
                aggregate.latestEndTime = order.getEndTime();
            }
        }

        List<DurationRankAggregate> sortedAggregates = aggregateMap.values().stream()
            .sorted(
                Comparator.comparingLong(DurationRankAggregate::getTotalSeconds).reversed()
                    .thenComparing(DurationRankAggregate::getLatestEndTime, Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(DurationRankAggregate::getUserId)
            )
            .toList();

        Map<Long, UserInfo> userMap = buildRankingUserMap(sortedAggregates);
        List<Map<String, Object>> rows = new ArrayList<>();
        Map<String, Object> myRank = null;
        int rank = 1;
        for (DurationRankAggregate aggregate : sortedAggregates) {
            UserInfo user = userMap.get(aggregate.userId);
            Map<String, Object> item = toDurationRankItem(rank, aggregate, user);
            if (rank <= rowLimit) {
                rows.add(item);
            }
            if (userId != null && userId.equals(aggregate.userId)) {
                myRank = item;
            }
            rank++;
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("scope", resolvedScope);
        result.put("generatedAt", now);
        result.put("rows", rows);
        result.put("myRank", myRank);
        return result;
    }

    @Override
    public Page<AdminOrderListItem> pageAdminOrders(
        long page,
        long size,
        Long storeId,
        String orderStatus,
        String paymentStatus,
        String keyword,
        LocalDateTime startTime,
        LocalDateTime endTime
    ) {
        LambdaQueryWrapper<WashOrder> wrapper = new LambdaQueryWrapper<WashOrder>()
            .eq(storeId != null, WashOrder::getStoreId, storeId)
            .eq(StringUtils.hasText(orderStatus), WashOrder::getOrderStatus, orderStatus)
            .eq(StringUtils.hasText(paymentStatus), WashOrder::getPaymentStatus, paymentStatus)
            .ge(startTime != null, WashOrder::getCreatedAt, startTime)
            .le(endTime != null, WashOrder::getCreatedAt, endTime)
            .orderByDesc(WashOrder::getId);

        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w
                .like(WashOrder::getOrderNo, keyword)
                .or()
                .like(WashOrder::getRemark, keyword));
        }

        Page<WashOrder> orderPage = this.page(new Page<>(page, size), wrapper);
        Map<Long, Store> storeMap = buildStoreMap(orderPage.getRecords());
        Map<Long, Device> deviceMap = buildDeviceMap(orderPage.getRecords());

        Page<AdminOrderListItem> result = new Page<>(orderPage.getCurrent(), orderPage.getSize(), orderPage.getTotal());
        result.setRecords(orderPage.getRecords().stream()
            .map(order -> toAdminOrderListItem(order, storeMap, deviceMap))
            .toList());
        return result;
    }

    @Override
    public AdminOrderDetail getAdminOrderDetail(Long id) {
        WashOrder order = getRequiredOrder(id);
        Map<Long, Store> storeMap = buildStoreMap(List.of(order));
        Map<Long, Device> deviceMap = buildDeviceMap(List.of(order));
        ResolvedFirstPeriodDiscountView discountView = resolveFirstPeriodDiscountView(order);
        Store store = storeMap.get(order.getStoreId());
        Device device = deviceMap.get(order.getDeviceId());
        return new AdminOrderDetail(
            order.getId(),
            order.getOrderNo(),
            order.getUserId(),
            order.getStoreId(),
            store != null ? store.getStoreName() : "",
            order.getDeviceId(),
            device != null ? device.getDeviceCode() : "",
            device != null ? device.getDeviceName() : "",
            device != null ? device.getDeviceStatus() : "",
            order.getOrderSource(),
            order.getPayMode(),
            order.getPaymentStatus(),
            order.getPaymentStatusDesc(),
            order.getOrderStatus(),
            order.getEstimatedAmount(),
            resolveDisplayFinalAmount(order, discountView),
            order.getPaidAmount(),
            order.getRefundAmount(),
            discountView.used() ? 1 : 0,
            discountView.discountAmount(),
            discountView.description(),
            order.getCardUsageId(),
            order.getCardDeductTimes(),
            order.getRemark(),
            order.getAbnormalReason(),
            order.getCreatedAt(),
            order.getStartTime(),
            order.getEndTime(),
            order.getSettleTime(),
            getStatusLogs(id),
            getPaymentDetails(id)
        );
    }

    private FirstPeriodDiscountResult applyFirstPeriodDiscountOnStart(WashOrder order) {
        LocalDate discountDate = LocalDate.now();
        BigDecimal baseAmount = getBaseAmount(order);
        UserDailyDiscountRecord existingRecord = findFirstPeriodDiscountRecord(order, discountDate);

        if (existingRecord != null) {
            if (existingRecord.getOrderId() != null && existingRecord.getOrderId().equals(order.getId())) {
                BigDecimal recordedDiscountAmount = normalizeAmount(existingRecord.getDiscountAmount());
                BigDecimal repairedDiscountAmount = recordedDiscountAmount.compareTo(BigDecimal.ZERO) > 0
                    ? recordedDiscountAmount
                    : calculateFirstPeriodDiscountAmount(baseAmount);

                if (recordedDiscountAmount.compareTo(repairedDiscountAmount) != 0) {
                    existingRecord.setDiscountAmount(repairedDiscountAmount);
                    userDailyDiscountRecordMapper.updateById(existingRecord);
                }

                return buildDiscountHitResult(baseAmount, repairedDiscountAmount, "开始订单，恢复首段优惠");
            }

            return buildDiscountMissResult(baseAmount, "开始订单，当日首段优惠已被占用");
        }

        BigDecimal discountAmount = calculateFirstPeriodDiscountAmount(baseAmount);
        if (discountAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return buildDiscountMissResult(baseAmount, "开始订单，未命中首段优惠");
        }

        UserDailyDiscountRecord record = new UserDailyDiscountRecord();
        record.setUserId(order.getUserId());
        record.setDiscountDate(discountDate);
        record.setDiscountType(DISCOUNT_TYPE_FIRST_PERIOD);
        record.setDiscountScope(DISCOUNT_SCOPE_USER_STORE_DAY);
        record.setStoreId(order.getStoreId());
        record.setScopeStoreId(order.getStoreId());
        record.setOrderId(order.getId());
        record.setOrderNo(order.getOrderNo());
        record.setDiscountAmount(discountAmount);
        userDailyDiscountRecordMapper.insert(record);
        return buildDiscountHitResult(baseAmount, discountAmount, "开始订单，命中首段优惠");
    }

    private WashOrder repairDiscountFieldsBeforeSettle(WashOrder order) {
        ResolvedFirstPeriodDiscountView discountView = resolveFirstPeriodDiscountView(order);
        BigDecimal baseAmount = getBaseAmount(order);
        BigDecimal expectedFinalAmount = calculateFinalAmount(baseAmount, discountView.discountAmount());
        BigDecimal currentDiscountAmount = normalizeAmount(order.getFirstPeriodDiscountAmount());
        BigDecimal currentFinalAmount = normalizeAmount(order.getFinalAmount());
        boolean orderUsed = order.getIsFirstPeriodDiscountUsed() != null && order.getIsFirstPeriodDiscountUsed() == 1;

        boolean needsRepair = discountView.used() && (
            !orderUsed
                || currentDiscountAmount.compareTo(discountView.discountAmount()) != 0
                || currentFinalAmount.compareTo(expectedFinalAmount) != 0
        );

        if (!needsRepair) {
            return order;
        }

        LambdaUpdateWrapper<WashOrder> wrapper = new LambdaUpdateWrapper<WashOrder>()
            .eq(WashOrder::getId, order.getId())
            .set(WashOrder::getIsFirstPeriodDiscountUsed, 1)
            .set(WashOrder::getFirstPeriodDiscountAmount, discountView.discountAmount())
            .set(WashOrder::getFinalAmount, expectedFinalAmount);
        this.update(wrapper);
        return getRequiredOrder(order.getId());
    }

    private ResolvedFirstPeriodDiscountView resolveFirstPeriodDiscountView(WashOrder order) {
        BigDecimal orderDiscountAmount = normalizeAmount(order.getFirstPeriodDiscountAmount());
        boolean orderUsed = order.getIsFirstPeriodDiscountUsed() != null && order.getIsFirstPeriodDiscountUsed() == 1;
        LocalDate discountDate = resolveDiscountDate(order);
        UserDailyDiscountRecord existingRecord = findFirstPeriodDiscountRecord(order, discountDate);
        boolean started = order.getStartTime() != null;

        if (orderUsed) {
            BigDecimal effectiveDiscountAmount = orderDiscountAmount.compareTo(BigDecimal.ZERO) > 0
                ? orderDiscountAmount
                : calculateFirstPeriodDiscountAmount(getBaseAmount(order));
            return new ResolvedFirstPeriodDiscountView(
                true,
                effectiveDiscountAmount,
                orderDiscountAmount.compareTo(BigDecimal.ZERO) > 0
                    ? "已命中首段优惠"
                    : "已命中首段优惠，但订单字段未完整回写"
            );
        }

        if (existingRecord == null) {
            return new ResolvedFirstPeriodDiscountView(
                false,
                BigDecimal.ZERO,
                started ? "未命中首段优惠" : "订单未开始，尚未锁定首段优惠资格"
            );
        }

        if (existingRecord.getOrderId() != null && existingRecord.getOrderId().equals(order.getId())) {
            BigDecimal recordedDiscountAmount = normalizeAmount(existingRecord.getDiscountAmount());
            BigDecimal effectiveDiscountAmount = recordedDiscountAmount.compareTo(BigDecimal.ZERO) > 0
                ? recordedDiscountAmount
                : calculateFirstPeriodDiscountAmount(getBaseAmount(order));
            return new ResolvedFirstPeriodDiscountView(
                true,
                effectiveDiscountAmount,
                "已命中首段优惠，但订单历史字段未完整回写"
            );
        }

        return new ResolvedFirstPeriodDiscountView(
            false,
            BigDecimal.ZERO,
            started ? "当日该门店首段优惠已被其他订单使用" : "订单未开始；当前当日当店优惠资格已被其他订单占用"
        );
    }

    private UserDailyDiscountRecord findFirstPeriodDiscountRecord(WashOrder order, LocalDate discountDate) {
        return userDailyDiscountRecordMapper.selectOne(
            new LambdaQueryWrapper<UserDailyDiscountRecord>()
                .eq(UserDailyDiscountRecord::getUserId, order.getUserId())
                .eq(UserDailyDiscountRecord::getDiscountDate, discountDate)
                .eq(UserDailyDiscountRecord::getDiscountType, DISCOUNT_TYPE_FIRST_PERIOD)
                .eq(UserDailyDiscountRecord::getDiscountScope, DISCOUNT_SCOPE_USER_STORE_DAY)
                .eq(UserDailyDiscountRecord::getScopeStoreId, order.getStoreId())
                .last("limit 1")
        );
    }

    private LocalDate resolveDiscountDate(WashOrder order) {
        if (order.getStartTime() != null) {
            return order.getStartTime().toLocalDate();
        }
        return LocalDate.now();
    }

    private BigDecimal getBaseAmount(WashOrder order) {
        return normalizeAmount(order.getEstimatedAmount());
    }

    private WashPricingSnapshot resolvePricingSnapshotForStart(Long userId, Long storeId, LocalDateTime startTime) {
        boolean vipPricing = hasActiveMonthlyCard(userId, storeId);
        boolean memberDayDiscount = !vipPricing && isRechargeMember(userId) && isMemberDay(startTime);
        return washPricingService.resolveSnapshotForStore(storeId, vipPricing, memberDayDiscount);
    }

    private BigDecimal calculateWashOrderAmount(WashOrder order, LocalDateTime endTime) {
        return washPricingService.calculateAmount(order, endTime);
    }

    private void ensureBayIsIdle(WashOrder order) {
        if (order.getDeviceId() == null && order.getBayId() == null) {
            return;
        }
        LambdaQueryWrapper<WashOrder> wrapper = new LambdaQueryWrapper<WashOrder>()
            .eq(WashOrder::getStoreId, order.getStoreId())
            .eq(WashOrder::getOrderStatus, STATUS_RUNNING)
            .ne(order.getId() != null, WashOrder::getId, order.getId());
        wrapper.and(w -> {
            if (order.getDeviceId() != null) {
                w.eq(WashOrder::getDeviceId, order.getDeviceId());
            }
            if (order.getBayId() != null) {
                if (order.getDeviceId() != null) {
                    w.or();
                }
                w.eq(WashOrder::getBayId, order.getBayId());
            }
        });
        long runningCount = this.count(wrapper);
        if (runningCount > 0) {
            throw new IllegalArgumentException("bay is using");
        }
    }

    private void normalizeOrderTarget(SimpleOrderCreateRequest request) {
        if (request.getDeviceId() == null && request.getBayId() == null) {
            throw new IllegalArgumentException("deviceId or bayId is required");
        }

        Device device = null;
        if (request.getDeviceId() != null) {
            device = deviceService.getById(request.getDeviceId());
            if (device == null) {
                throw new IllegalArgumentException("device not found");
            }
            if (device.getStoreId() == null || !device.getStoreId().equals(request.getStoreId())) {
                throw new IllegalArgumentException("device does not belong to store");
            }
        }

        if (device == null && request.getBayId() != null) {
            Long bayId = request.getBayId();
            device = deviceService.lambdaQuery()
                .eq(Device::getStoreId, request.getStoreId())
                .and(w -> w.eq(Device::getBayId, bayId).or().eq(Device::getId, bayId))
                .last("limit 1")
                .one();
            if (device == null) {
                throw new IllegalArgumentException("device not found for store");
            }
        }

        Long resolvedBayId = resolveBayId(device);
        if (request.getBayId() != null && resolvedBayId != null && !request.getBayId().equals(resolvedBayId)) {
            throw new IllegalArgumentException("deviceId and bayId do not match");
        }

        validateDeviceCanStart(device);

        request.setDeviceId(device != null ? device.getId() : request.getDeviceId());
        request.setBayId(resolvedBayId != null ? resolvedBayId : request.getBayId());
    }

    private void validateDeviceCanStart(Device device) {
        if (device == null) {
            return;
        }
        String status = StringUtils.hasText(device.getDeviceStatus())
            ? device.getDeviceStatus().trim().toLowerCase()
            : "";
        if (!StringUtils.hasText(status) || "idle".equals(status) || "online".equals(status)) {
            return;
        }
        if ("offline".equals(status)
            || "fault".equals(status)
            || "disabled".equals(status)
            || "running".equals(status)
            || "paused".equals(status)) {
            throw new IllegalArgumentException("device is " + status);
        }
        throw new IllegalArgumentException("device is unavailable");
    }

    private Long resolveBayId(Device device) {
        if (device == null) {
            return null;
        }
        return device.getBayId() != null ? device.getBayId() : device.getId();
    }

    private BigDecimal normalizeAmount(BigDecimal amount) {
        return amount != null ? amount : BigDecimal.ZERO;
    }

    private BigDecimal calculateFirstPeriodDiscountAmount(BigDecimal baseAmount) {
        BigDecimal normalizedBaseAmount = normalizeAmount(baseAmount);
        if (normalizedBaseAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return normalizedBaseAmount.min(DEFAULT_FIRST_PERIOD_DISCOUNT_AMOUNT);
    }

    private BigDecimal calculateFinalAmount(BigDecimal baseAmount, BigDecimal discountAmount) {
        BigDecimal normalizedBaseAmount = normalizeAmount(baseAmount);
        BigDecimal normalizedDiscountAmount = normalizeAmount(discountAmount);
        BigDecimal finalAmount = normalizedBaseAmount.subtract(normalizedDiscountAmount);
        return finalAmount.compareTo(BigDecimal.ZERO) >= 0 ? finalAmount : BigDecimal.ZERO;
    }

    private FirstPeriodDiscountResult buildDiscountHitResult(BigDecimal baseAmount, BigDecimal discountAmount, String logRemark) {
        return new FirstPeriodDiscountResult(
            true,
            normalizeAmount(discountAmount),
            calculateFinalAmount(baseAmount, discountAmount),
            logRemark
        );
    }

    private FirstPeriodDiscountResult buildDiscountMissResult(BigDecimal baseAmount, String logRemark) {
        return new FirstPeriodDiscountResult(false, BigDecimal.ZERO, normalizeAmount(baseAmount), logRemark);
    }

    private BigDecimal resolveDisplayFinalAmount(WashOrder order, ResolvedFirstPeriodDiscountView discountView) {
        if (!discountView.used()) {
            return order.getFinalAmount();
        }

        if ("paid".equals(order.getPaymentStatus())) {
            return order.getFinalAmount();
        }

        return calculateFinalAmount(getBaseAmount(order), discountView.discountAmount());
    }

    private BigDecimal resolveSimpleListAmount(WashOrder order) {
        if (PAY_MODE_CARD.equals(order.getPayMode())) {
            return BigDecimal.ZERO;
        }

        BigDecimal finalAmount = normalizeAmount(order.getFinalAmount());
        if (finalAmount.compareTo(BigDecimal.ZERO) > 0) {
            return finalAmount;
        }

        BigDecimal estimatedAmount = normalizeAmount(order.getEstimatedAmount());
        if (estimatedAmount.compareTo(BigDecimal.ZERO) > 0) {
            return estimatedAmount;
        }

        return normalizeAmount(order.getPaidAmount());
    }

    private boolean shouldAutoStopCardOrder(WashOrder order, LocalDateTime now) {
        if (order == null || !PAY_MODE_CARD.equals(order.getPayMode())) {
            return false;
        }
        LocalDateTime startTime = order.getStartTime();
        if (startTime == null) {
            return false;
        }
        LocalDateTime deadline = startTime.plusMinutes(CARD_ORDER_LIMIT_MINUTES);
        return !deadline.isAfter(now != null ? now : LocalDateTime.now());
    }

    private WashOrder refreshRunningOrderForDisplay(WashOrder order) {
        if (order == null || !STATUS_RUNNING.equals(order.getOrderStatus()) || order.getId() == null) {
            return order;
        }
        return checkAndAutoStopOrder(order.getId());
    }

    private List<UserStoreWallet> getUserWallets(Long userId) {
        return userStoreWalletMapper.selectList(
            new LambdaQueryWrapper<UserStoreWallet>()
                .eq(UserStoreWallet::getUserId, userId)
                .orderByAsc(UserStoreWallet::getStoreId)
                .orderByAsc(UserStoreWallet::getId)
        );
    }

    private BigDecimal calculateWalletAvailableForOrder(WashOrder order) {
        List<UserStoreWallet> activeWallets = getUserWallets(order.getUserId()).stream()
            .filter(this::isWalletActive)
            .toList();
        if (activeWallets.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal principalAvailable = activeWallets.stream()
            .map(this::resolveAvailablePrincipal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal giftAvailable = activeWallets.stream()
            .filter(wallet -> order.getStoreId() != null && order.getStoreId().equals(wallet.getStoreId()))
            .findFirst()
            .map(this::resolveAvailableGift)
            .orElse(BigDecimal.ZERO);

        BigDecimal totalAvailable = principalAvailable.add(giftAvailable);
        if (totalAvailable.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }
        return totalAvailable.setScale(2, RoundingMode.HALF_UP);
    }

    private void ensureWalletCanCoverBaseAmount(Long userId, Long storeId, BigDecimal baseAmount) {
        BigDecimal requiredAmount = normalizeAmount(baseAmount);
        if (requiredAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        BigDecimal walletAvailable = calculateWalletAvailableForStart(userId, storeId);
        if (walletAvailable.compareTo(requiredAmount) < 0) {
            throw new IllegalArgumentException(
                "钱包余额不足，至少需要" + formatAmount(requiredAmount) + "元才可开始洗车"
            );
        }
    }

    private BigDecimal calculateWalletAvailableForStart(Long userId, Long storeId) {
        if (userId == null) {
            return BigDecimal.ZERO;
        }
        List<UserStoreWallet> activeWallets = getUserWallets(userId).stream()
            .filter(this::isWalletActive)
            .toList();
        if (activeWallets.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal principalAvailable = activeWallets.stream()
            .map(this::resolveAvailablePrincipal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal giftAvailable = activeWallets.stream()
            .filter(wallet -> storeId != null && storeId.equals(wallet.getStoreId()))
            .findFirst()
            .map(this::resolveAvailableGift)
            .orElse(BigDecimal.ZERO);

        return principalAvailable.add(giftAvailable).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    private String formatAmount(BigDecimal amount) {
        return normalizeAmount(amount).stripTrailingZeros().toPlainString();
    }

    private String resolvePayModeForCreate(Long userId, Long storeId, String requestedPayMode) {
        String normalizedPayMode = StringUtils.hasText(requestedPayMode)
            ? requestedPayMode.trim().toLowerCase()
            : "";

        if (!StringUtils.hasText(normalizedPayMode)) {
            return PAY_MODE_WALLET;
        }

        if (PAY_MODE_CARD.equals(normalizedPayMode)) {
            UserCard availableCard = findAvailableCard(userId, storeId, false);
            if (availableCard == null) {
                throw new IllegalArgumentException("available user card not found");
            }
            return PAY_MODE_CARD;
        }

        if (PAY_MODE_WALLET.equals(normalizedPayMode)) {
            return PAY_MODE_WALLET;
        }

        throw new IllegalArgumentException("unsupported pay_mode");
    }

    private UserCard getRequiredAvailableCard(Long userId, Long storeId) {
        UserCard userCard = findAvailableCard(userId, storeId, true);
        if (userCard == null) {
            throw new IllegalArgumentException("available user card not found");
        }
        return userCard;
    }

    private UserCard lockAvailableCardForOrder(WashOrder order) {
        UserCard userCard = getRequiredAvailableCard(order.getUserId(), order.getStoreId());
        int updatedRows = userCardMapper.update(
            null,
            new LambdaUpdateWrapper<UserCard>()
                .eq(UserCard::getId, userCard.getId())
                .eq(UserCard::getStatus, "active")
                .gt(UserCard::getRemainingTimes, 0)
                .set(UserCard::getStatus, "locked")
        );
        if (updatedRows <= 0) {
            throw new IllegalStateException("available user card changed while starting order");
        }
        userCard.setStatus("locked");
        return userCard;
    }

    private UserCard getRequiredLockedCard(WashOrder order) {
        if (order.getCardUsageId() == null) {
            return lockAvailableCardForOrder(order);
        }

        UserCard userCard = userCardMapper.selectOne(
            new LambdaQueryWrapper<UserCard>()
                .eq(UserCard::getId, order.getCardUsageId())
                .eq(UserCard::getUserId, order.getUserId())
                .eq(UserCard::getStoreId, order.getStoreId())
                .eq(UserCard::getStatus, "locked")
                .last("limit 1 for update")
        );
        if (userCard == null) {
            throw new IllegalArgumentException("locked user card not found");
        }
        return userCard;
    }

    private void releaseLockedCard(WashOrder order) {
        if (order.getCardUsageId() == null) {
            return;
        }
        userCardMapper.update(
            null,
            new LambdaUpdateWrapper<UserCard>()
                .eq(UserCard::getId, order.getCardUsageId())
                .eq(UserCard::getUserId, order.getUserId())
                .eq(UserCard::getStoreId, order.getStoreId())
                .eq(UserCard::getStatus, "locked")
                .set(UserCard::getStatus, "active")
        );
    }

    private UserCard findAvailableCard(Long userId, Long storeId, boolean forUpdate) {
        if (userId == null || storeId == null) {
            return null;
        }

        LocalDateTime now = LocalDateTime.now();
        return userCardMapper.selectOne(
            new LambdaQueryWrapper<UserCard>()
                .eq(UserCard::getUserId, userId)
                .eq(UserCard::getStoreId, storeId)
                .eq(UserCard::getStatus, "active")
                .gt(UserCard::getRemainingTimes, 0)
                .and(wrapper -> wrapper.isNull(UserCard::getEffectiveTime).or().le(UserCard::getEffectiveTime, now))
                .and(wrapper -> wrapper.isNull(UserCard::getExpireTime).or().gt(UserCard::getExpireTime, now))
                .orderByAsc(UserCard::getId)
                .last(forUpdate ? "limit 1 for update" : "limit 1")
        );
    }

    private boolean hasActiveMonthlyCard(Long userId, Long storeId) {
        if (userId == null || storeId == null) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        Long count = userCardMapper.selectCount(
            new LambdaQueryWrapper<UserCard>()
                .eq(UserCard::getUserId, userId)
                .eq(UserCard::getStoreId, storeId)
                .eq(UserCard::getCardType, MONTHLY_CARD_TYPE)
                .eq(UserCard::getStatus, "active")
                .and(wrapper -> wrapper.isNull(UserCard::getEffectiveTime).or().le(UserCard::getEffectiveTime, now))
                .and(wrapper -> wrapper.isNull(UserCard::getExpireTime).or().gt(UserCard::getExpireTime, now))
        );
        return count != null && count > 0;
    }

    private boolean isRechargeMember(Long userId) {
        if (userId == null) {
            return false;
        }
        UserInfo user = userInfoService.getById(userId);
        if (user != null && Integer.valueOf(1).equals(user.getIsMember())) {
            return true;
        }
        if (!hasRechargeHistory(userId)) {
            return false;
        }
        if (user != null) {
            user.setIsMember(1);
            if (!StringUtils.hasText(user.getMemberLevel())) {
                user.setMemberLevel("normal");
            }
            if (user.getMemberSinceTime() == null) {
                user.setMemberSinceTime(LocalDateTime.now());
            }
            userInfoService.updateById(user);
        }
        return true;
    }

    private boolean isMemberDay(LocalDateTime time) {
        LocalDateTime safeTime = time != null ? time : LocalDateTime.now();
        return washPricingService.isMemberDay(safeTime.toLocalDate());
    }

    private boolean hasRechargeHistory(Long userId) {
        Long count = walletTransactionMapper.selectCount(
            new LambdaQueryWrapper<WalletTransaction>()
                .eq(WalletTransaction::getUserId, userId)
                .eq(WalletTransaction::getBizType, "recharge")
                .eq(WalletTransaction::getChangeType, "in")
        );
        return count != null && count > 0;
    }

    private WashOrder getRequiredOrderForUpdate(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("order id is required");
        }
        WashOrder order = this.lambdaQuery()
            .eq(WashOrder::getId, id)
            .last("limit 1 for update")
            .one();
        if (order == null) {
            throw new IllegalArgumentException("order not found");
        }
        return order;
    }

    private WashOrder getRequiredOrder(Long id) {
        WashOrder order = this.getById(id);
        if (order == null) {
            throw new IllegalArgumentException("order not found");
        }
        return order;
    }

    private void ensureOrderCanStart(WashOrder order) {
        if (order == null) {
            throw new IllegalArgumentException("order not found");
        }
        if (!STATUS_PENDING.equals(order.getOrderStatus())) {
            throw new IllegalArgumentException("order cannot start from status " + order.getOrderStatus());
        }
        if (order.getUserId() == null) {
            throw new IllegalArgumentException("userId is required");
        }
        if (order.getStoreId() == null) {
            throw new IllegalArgumentException("storeId is required");
        }
        if (order.getDeviceId() == null && order.getBayId() == null) {
            throw new IllegalArgumentException("deviceId or bayId is required");
        }
    }

    private void ensureOrderCanComplete(WashOrder order) {
        if (order == null) {
            throw new IllegalArgumentException("order not found");
        }
        if (!STATUS_RUNNING.equals(order.getOrderStatus())) {
            throw new IllegalArgumentException("order cannot complete from status " + order.getOrderStatus());
        }
    }

    private Device lockRequiredDeviceForStart(WashOrder order) {
        Device device = lockDeviceForOrder(order);
        if (device == null) {
            throw new IllegalArgumentException("device not found");
        }
        if (device.getStoreId() == null || !device.getStoreId().equals(order.getStoreId())) {
            throw new IllegalArgumentException("device does not belong to store");
        }

        Long resolvedBayId = resolveBayId(device);
        if (order.getBayId() != null && resolvedBayId != null && !order.getBayId().equals(resolvedBayId)) {
            throw new IllegalArgumentException("deviceId and bayId do not match");
        }

        order.setDeviceId(device.getId());
        order.setBayId(resolvedBayId != null ? resolvedBayId : order.getBayId());
        validateDeviceCanStart(device);
        return device;
    }

    private Device lockDeviceForOrder(WashOrder order) {
        if (order == null || order.getStoreId() == null) {
            return null;
        }

        if (order.getDeviceId() != null) {
            return deviceService.lambdaQuery()
                .eq(Device::getId, order.getDeviceId())
                .last("limit 1 for update")
                .one();
        }

        if (order.getBayId() == null) {
            return null;
        }

        return deviceService.lambdaQuery()
            .eq(Device::getStoreId, order.getStoreId())
            .and(w -> w.eq(Device::getBayId, order.getBayId()).or().eq(Device::getId, order.getBayId()))
            .last("limit 1 for update")
            .one();
    }

    private void markOrderDeviceIdle(WashOrder order) {
        Device device = lockDeviceForOrder(order);
        if (device != null) {
            markDeviceStatus(device, "idle");
        }
    }

    private void markDeviceStatus(Device device, String status) {
        if (device == null || device.getId() == null || !StringUtils.hasText(status)) {
            return;
        }
        deviceService.update(
            new LambdaUpdateWrapper<Device>()
                .eq(Device::getId, device.getId())
                .set(Device::getDeviceStatus, status)
        );
        device.setDeviceStatus(status);
    }

    private String buildCommandNo(String commandType, WashOrder order) {
        String type = StringUtils.hasText(commandType) ? commandType.trim().toUpperCase() : "CMD";
        String orderPart = order != null && order.getId() != null ? String.valueOf(order.getId()) : "0";
        return type + orderPart + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    private Map<Long, Store> buildStoreMap(List<WashOrder> orders) {
        List<Long> storeIds = orders.stream()
            .map(WashOrder::getStoreId)
            .filter(id -> id != null)
            .distinct()
            .toList();

        if (storeIds.isEmpty()) {
            return Map.of();
        }

        return storeService.listByIds(storeIds).stream()
            .collect(Collectors.toMap(Store::getId, Function.identity(), (a, b) -> a));
    }

    private Map<Long, Device> buildDeviceMap(List<WashOrder> orders) {
        List<Long> deviceIds = orders.stream()
            .map(WashOrder::getDeviceId)
            .filter(id -> id != null)
            .distinct()
            .toList();

        if (deviceIds.isEmpty()) {
            return Map.of();
        }

        return deviceService.listByIds(deviceIds).stream()
            .collect(Collectors.toMap(Device::getId, Function.identity(), (a, b) -> a));
    }

    private AdminOrderListItem toAdminOrderListItem(WashOrder order, Map<Long, Store> storeMap, Map<Long, Device> deviceMap) {
        Store store = storeMap.get(order.getStoreId());
        Device device = deviceMap.get(order.getDeviceId());
        return new AdminOrderListItem(
            order.getId(),
            order.getOrderNo(),
            order.getUserId(),
            order.getStoreId(),
            store != null ? store.getStoreName() : "",
            order.getDeviceId(),
            device != null ? device.getDeviceName() : "",
            order.getPayMode(),
            order.getPaymentStatus(),
            order.getOrderStatus(),
            order.getFinalAmount(),
            order.getPaidAmount(),
            order.getRemark(),
            order.getCreatedAt(),
            order.getStartTime(),
            order.getEndTime()
        );
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

    private void insertWalletTransaction(
        WashOrder order,
        Long sourceStoreId,
        String amountType,
        BigDecimal balanceBefore,
        BigDecimal balanceAfter,
        BigDecimal amount,
        String bizActionNo
    ) {
        WalletTransaction transaction = new WalletTransaction();
        transaction.setTransactionNo("WT" + UUID.randomUUID().toString().replace("-", "").substring(0, 18));
        transaction.setUserId(order.getUserId());
        transaction.setStoreId(sourceStoreId);
        transaction.setBizType("consume");
        transaction.setAmountType(amountType);
        transaction.setBalanceBucket("available");
        transaction.setChangeType("out");
        transaction.setAmount(amount);
        transaction.setRelatedAction("consume");
        transaction.setBizActionNo(bizActionNo);
        transaction.setBalanceBefore(balanceBefore);
        transaction.setBalanceAfter(balanceAfter);
        transaction.setRelatedOrderId(order.getId());
        transaction.setRelatedOrderNo(order.getOrderNo());
        transaction.setRemark(
            "gift".equals(amountType)
                ? "wallet gift pay for wash order"
                : "wallet principal pay for wash order"
        );
        walletTransactionMapper.insert(transaction);
    }


    private void insertOrderPaymentDetail(
        WashOrder order,
        Long sourceStoreId,
        String amountType,
        BigDecimal amount,
        String bizActionNo,
        int paymentSeq
    ) {
        WashOrderPaymentDetail detail = new WashOrderPaymentDetail();
        detail.setOrderId(order.getId());
        detail.setOrderNo(order.getOrderNo());
        detail.setUserId(order.getUserId());
        detail.setConsumeStoreId(order.getStoreId());
        detail.setSourceType("wallet");
        detail.setSourceStoreId(sourceStoreId);
        detail.setAmountType(amountType);
        detail.setAmount(amount);
        detail.setDeductTimes(0);
        detail.setPaymentSeq(paymentSeq);
        detail.setSettleStage("final");
        detail.setAllocationStrategy("manual");
        detail.setBizActionNo(bizActionNo);
        detail.setRefundedAmount(BigDecimal.ZERO);
        washOrderPaymentDetailMapper.insert(detail);
    }

    private void generateSettlementDetails(WashOrder order) {
        if (order == null || order.getId() == null) {
            return;
        }
        List<WashOrderPaymentDetail> details = washOrderPaymentDetailMapper.selectList(
            new LambdaQueryWrapper<WashOrderPaymentDetail>()
                .eq(WashOrderPaymentDetail::getOrderId, order.getId())
                .orderByAsc(WashOrderPaymentDetail::getId)
        );
        if (details.isEmpty()) {
            return;
        }

        LocalDate bizDate = resolveSettlementBizDate(order);
        for (WashOrderPaymentDetail detail : details) {
            if (!"wallet".equals(detail.getSourceType())) {
                continue;
            }
            if (!"principal".equals(detail.getAmountType())) {
                continue;
            }
            Long sourceStoreId = detail.getSourceStoreId();
            Long consumeStoreId = detail.getConsumeStoreId();
            if (sourceStoreId == null || consumeStoreId == null || sourceStoreId.equals(consumeStoreId)) {
                continue;
            }
            BigDecimal amount = normalizeAmount(detail.getAmount());
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            StoreSettlementDetail existing = storeSettlementDetailMapper.selectOne(
                new LambdaQueryWrapper<StoreSettlementDetail>()
                    .eq(StoreSettlementDetail::getPaymentDetailId, detail.getId())
                    .last("limit 1")
            );
            if (existing != null) {
                continue;
            }

            StoreSettlementDetail settlementDetail = new StoreSettlementDetail();
            settlementDetail.setFromStoreId(sourceStoreId);
            settlementDetail.setToStoreId(consumeStoreId);
            settlementDetail.setUserId(order.getUserId());
            settlementDetail.setOrderId(order.getId());
            settlementDetail.setOrderNo(order.getOrderNo());
            settlementDetail.setPaymentDetailId(detail.getId());
            settlementDetail.setPrincipalAmount(amount);
            settlementDetail.setRefundAdjustAmount(BigDecimal.ZERO);
            settlementDetail.setNetAmount(amount);
            settlementDetail.setBizDate(bizDate);
            settlementDetail.setDetailStatus("pending");
            storeSettlementDetailMapper.insert(settlementDetail);
        }
    }

    private LocalDate resolveSettlementBizDate(WashOrder order) {
        LocalDateTime settleTime = order.getSettleTime();
        if (settleTime != null) {
            return settleTime.toLocalDate();
        }
        LocalDateTime createdAt = order.getCreatedAt();
        if (createdAt != null) {
            return createdAt.toLocalDate();
        }
        return LocalDate.now();
    }

    private WalletAllocation buildWalletAllocation(WashOrder order, BigDecimal finalAmount) {
        if (finalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return new WalletAllocation(List.of(), BigDecimal.ZERO, false);
        }

        List<UserStoreWallet> wallets = getUserWallets(order.getUserId());
        if (wallets.isEmpty()) {
            throw new IllegalArgumentException("wallet not found");
        }

        List<UserStoreWallet> activeWallets = wallets.stream()
            .filter(this::isWalletActive)
            .toList();

        if (activeWallets.isEmpty()) {
            throw new IllegalArgumentException("wallet is frozen");
        }

        UserStoreWallet consumeWallet = activeWallets.stream()
            .filter(wallet -> order.getStoreId() != null && order.getStoreId().equals(wallet.getStoreId()))
            .findFirst()
            .orElse(null);

        Comparator<UserStoreWallet> walletOrder = Comparator
            .comparing(UserStoreWallet::getStoreId, Comparator.nullsLast(Long::compareTo))
            .thenComparing(UserStoreWallet::getId, Comparator.nullsLast(Long::compareTo));

        List<UserStoreWallet> otherWallets = activeWallets.stream()
            .filter(wallet -> consumeWallet == null || !wallet.getId().equals(consumeWallet.getId()))
            .sorted(walletOrder)
            .toList();

        List<WalletDeduction> deductions = new ArrayList<>();
        BigDecimal remaining = finalAmount;

        remaining = appendPrincipalDeduction(consumeWallet, remaining, deductions);
        for (UserStoreWallet wallet : otherWallets) {
            remaining = appendPrincipalDeduction(wallet, remaining, deductions);
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
        }

        boolean usedGift = false;
        if (remaining.compareTo(BigDecimal.ZERO) > 0) {
            int beforeGiftSize = deductions.size();
            remaining = appendGiftDeduction(consumeWallet, remaining, deductions);
            usedGift = deductions.size() > beforeGiftSize;
        }

        if (remaining.compareTo(BigDecimal.ZERO) > 0) {
            throw new IllegalArgumentException("wallet balance is not enough");
        }

        BigDecimal total = deductions.stream()
            .map(WalletDeduction::amount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new WalletAllocation(deductions, total, usedGift);
    }

    private BigDecimal appendPrincipalDeduction(
        UserStoreWallet wallet,
        BigDecimal remaining,
        List<WalletDeduction> deductions
    ) {
        if (wallet == null || remaining.compareTo(BigDecimal.ZERO) <= 0) {
            return remaining;
        }
        BigDecimal availablePrincipal = resolveAvailablePrincipal(wallet);
        if (availablePrincipal.compareTo(BigDecimal.ZERO) <= 0) {
            return remaining;
        }
        BigDecimal amount = remaining.min(availablePrincipal);
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return remaining;
        }
        deductions.add(new WalletDeduction(
            wallet,
            "principal",
            amount,
            availablePrincipal,
            availablePrincipal.subtract(amount)
        ));
        return remaining.subtract(amount);
    }

    private BigDecimal appendGiftDeduction(
        UserStoreWallet wallet,
        BigDecimal remaining,
        List<WalletDeduction> deductions
    ) {
        if (wallet == null || remaining.compareTo(BigDecimal.ZERO) <= 0) {
            return remaining;
        }
        BigDecimal availableGift = resolveAvailableGift(wallet);
        if (availableGift.compareTo(BigDecimal.ZERO) <= 0) {
            return remaining;
        }
        BigDecimal amount = remaining.min(availableGift);
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return remaining;
        }
        deductions.add(new WalletDeduction(
            wallet,
            "gift",
            amount,
            availableGift,
            availableGift.subtract(amount)
        ));
        return remaining.subtract(amount);
    }

    private void applyWalletDeduction(
        WashOrder order,
        WalletDeduction deduction,
        String bizActionNo,
        int paymentSeq
    ) {
        if ("gift".equals(deduction.amountType())) {
            applyGiftDeduction(deduction.wallet(), deduction.amount());
        } else {
            applyPrincipalDeduction(deduction.wallet(), deduction.amount());
        }

        insertWalletTransaction(
            order,
            deduction.wallet().getStoreId(),
            deduction.amountType(),
            deduction.balanceBefore(),
            deduction.balanceAfter(),
            deduction.amount(),
            bizActionNo
        );
        insertOrderPaymentDetail(
            order,
            deduction.wallet().getStoreId(),
            deduction.amountType(),
            deduction.amount(),
            bizActionNo,
            paymentSeq
        );
    }


    private void applyPrincipalDeduction(UserStoreWallet wallet, BigDecimal amount) {
        BigDecimal availablePrincipal = resolveAvailablePrincipal(wallet);
        BigDecimal principalBalance = wallet.getPrincipalBalance() != null
            ? wallet.getPrincipalBalance()
            : availablePrincipal;
        BigDecimal totalConsumePrincipal = wallet.getTotalConsumePrincipal() != null
            ? wallet.getTotalConsumePrincipal()
            : BigDecimal.ZERO;
        BigDecimal balanceAfter = availablePrincipal.subtract(amount);
        BigDecimal principalAfter = principalBalance.subtract(amount);

        LambdaUpdateWrapper<UserStoreWallet> walletWrapper = new LambdaUpdateWrapper<UserStoreWallet>()
            .eq(UserStoreWallet::getId, wallet.getId())
            .set(UserStoreWallet::getPrincipalBalance, principalAfter)
            .set(UserStoreWallet::getAvailablePrincipalBalance, balanceAfter)
            .set(UserStoreWallet::getTotalConsumePrincipal, totalConsumePrincipal.add(amount));
        userStoreWalletMapper.update(null, walletWrapper);
    }

    private void applyGiftDeduction(UserStoreWallet wallet, BigDecimal amount) {
        BigDecimal availableGift = resolveAvailableGift(wallet);
        BigDecimal giftBalance = wallet.getGiftBalance() != null
            ? wallet.getGiftBalance()
            : availableGift;
        BigDecimal totalConsumeGift = wallet.getTotalConsumeGift() != null
            ? wallet.getTotalConsumeGift()
            : BigDecimal.ZERO;
        BigDecimal balanceAfter = availableGift.subtract(amount);
        BigDecimal giftAfter = giftBalance.subtract(amount);

        LambdaUpdateWrapper<UserStoreWallet> walletWrapper = new LambdaUpdateWrapper<UserStoreWallet>()
            .eq(UserStoreWallet::getId, wallet.getId())
            .set(UserStoreWallet::getGiftBalance, giftAfter)
            .set(UserStoreWallet::getAvailableGiftBalance, balanceAfter)
            .set(UserStoreWallet::getTotalConsumeGift, totalConsumeGift.add(amount));
        userStoreWalletMapper.update(null, walletWrapper);
    }

    private boolean isWalletActive(UserStoreWallet wallet) {
        return wallet.getStatus() == null || wallet.getStatus() != 0;
    }

    private BigDecimal resolveAvailablePrincipal(UserStoreWallet wallet) {
        if (wallet == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal available = wallet.getAvailablePrincipalBalance();
        if (available != null) {
            return normalizeAmount(available);
        }
        return normalizeAmount(wallet.getPrincipalBalance());
    }

    private BigDecimal resolveAvailableGift(UserStoreWallet wallet) {
        if (wallet == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal available = wallet.getAvailableGiftBalance();
        if (available != null) {
            return normalizeAmount(available);
        }
        return normalizeAmount(wallet.getGiftBalance());
    }

    private record WalletDeduction(
        UserStoreWallet wallet,
        String amountType,
        BigDecimal amount,
        BigDecimal balanceBefore,
        BigDecimal balanceAfter
    ) {
    }

    private record WalletAllocation(
        List<WalletDeduction> deductions,
        BigDecimal totalAmount,
        boolean hasGift
    ) {
    }

    private CardUsageRecord insertCardUsageRecord(WashOrder order, UserCard userCard) {
        CardUsageRecord record = new CardUsageRecord();
        record.setUsageNo("CU" + UUID.randomUUID().toString().replace("-", "").substring(0, 18));
        record.setUserCardId(userCard.getId());
        record.setUserId(order.getUserId());
        record.setStoreId(order.getStoreId());
        record.setOrderId(order.getId());
        record.setOrderNo(order.getOrderNo());
        record.setUsedTimes(1);
        record.setUsageTime(LocalDateTime.now());
        record.setOperatorType("user");
        record.setOperatorId(order.getUserId());
        record.setRemark("card pay for wash order");
        cardUsageRecordMapper.insert(record);
        return record;
    }

    private void insertCardPaymentDetail(WashOrder order, BigDecimal amount, String bizActionNo, Long userCardId) {
        WashOrderPaymentDetail detail = new WashOrderPaymentDetail();
        detail.setOrderId(order.getId());
        detail.setOrderNo(order.getOrderNo());
        detail.setUserId(order.getUserId());
        detail.setConsumeStoreId(order.getStoreId());
        detail.setSourceType("card");
        detail.setSourceStoreId(order.getStoreId());
        detail.setAmountType("card");
        detail.setUserCardId(userCardId);
        detail.setAmount(amount);
        detail.setDeductTimes(1);
        detail.setPaymentSeq(1);
        detail.setSettleStage("final");
        detail.setAllocationStrategy("manual");
        detail.setBizActionNo(bizActionNo);
        detail.setRefundedAmount(BigDecimal.ZERO);
        washOrderPaymentDetailMapper.insert(detail);
    }

    private String resolveRankingScope(String scope) {
        if ("month".equalsIgnoreCase(scope)) {
            return "month";
        }
        if ("total".equalsIgnoreCase(scope)) {
            return "total";
        }
        return "day";
    }

    private LocalDateTime resolveRankingStartTime(String scope, LocalDateTime now) {
        if ("month".equals(scope)) {
            return now.minusDays(30);
        }
        if ("total".equals(scope)) {
            return null;
        }
        return now.minusHours(24);
    }

    private long calculateOrderDurationSeconds(WashOrder order) {
        if (order == null || order.getStartTime() == null || order.getEndTime() == null) {
            return 0L;
        }
        if (order.getEndTime().isBefore(order.getStartTime())) {
            return 0L;
        }
        return Math.max(0L, Duration.between(order.getStartTime(), order.getEndTime()).getSeconds());
    }

    private Map<Long, UserInfo> buildRankingUserMap(List<DurationRankAggregate> aggregates) {
        List<Long> userIds = aggregates.stream()
            .map(DurationRankAggregate::getUserId)
            .filter(id -> id != null)
            .distinct()
            .toList();
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return userInfoService.listByIds(userIds).stream()
            .collect(Collectors.toMap(UserInfo::getId, Function.identity(), (a, b) -> a));
    }

    private Map<String, Object> toDurationRankItem(
        int rank,
        DurationRankAggregate aggregate,
        UserInfo user
    ) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("rank", rank);
        item.put("userId", aggregate.userId);
        item.put("nickname", resolveRankingNickname(user, aggregate.userId));
        item.put("name", resolveRankingNickname(user, aggregate.userId));
        item.put("avatarUrl", user != null ? user.getAvatarUrl() : null);
        item.put("durationSeconds", aggregate.totalSeconds);
        item.put("durationMinutes", Math.max(1L, (aggregate.totalSeconds + 59L) / 60L));
        item.put("durationText", formatDurationText(aggregate.totalSeconds));
        item.put("orderCount", aggregate.orderCount);
        item.put("latestEndTime", aggregate.latestEndTime);
        return item;
    }

    private String resolveRankingNickname(UserInfo user, Long userId) {
        if (user != null && StringUtils.hasText(user.getNickname())) {
            return user.getNickname();
        }
        if (userId != null) {
            return "车友" + userId;
        }
        return "匿名车友";
    }

    private String formatDurationText(long seconds) {
        long minutes = Math.max(1L, (seconds + 59L) / 60L);
        long hours = minutes / 60L;
        long remainMinutes = minutes % 60L;
        if (hours > 0) {
            return String.format("%02d时%02d分", hours, remainMinutes);
        }
        return String.format("%02d分", remainMinutes);
    }

    private static class DurationRankAggregate {
        private final Long userId;
        private long totalSeconds;
        private int orderCount;
        private LocalDateTime latestEndTime;

        private DurationRankAggregate(Long userId) {
            this.userId = userId;
        }

        private Long getUserId() {
            return userId;
        }

        private long getTotalSeconds() {
            return totalSeconds;
        }

        private LocalDateTime getLatestEndTime() {
            return latestEndTime;
        }
    }

    private record FirstPeriodDiscountResult(
        boolean used,
        BigDecimal discountAmount,
        BigDecimal finalAmount,
        String logRemark
    ) {
    }

    private record ResolvedFirstPeriodDiscountView(
        boolean used,
        BigDecimal discountAmount,
        String description
    ) {
    }
}
