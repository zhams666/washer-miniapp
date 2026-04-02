package com.washer.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.washer.backend.dto.admin.AdminOrderDetail;
import com.washer.backend.dto.admin.AdminOrderListItem;
import com.washer.backend.dto.order.SimpleOrderCreateRequest;
import com.washer.backend.dto.order.SimpleOrderItem;
import com.washer.backend.entity.CardUsageRecord;
import com.washer.backend.entity.Device;
import com.washer.backend.entity.Store;
import com.washer.backend.entity.UserCard;
import com.washer.backend.entity.UserDailyDiscountRecord;
import com.washer.backend.entity.UserStoreWallet;
import com.washer.backend.entity.WashOrder;
import com.washer.backend.entity.WashOrderPaymentDetail;
import com.washer.backend.entity.WashOrderStatusLog;
import com.washer.backend.entity.WalletTransaction;
import com.washer.backend.mapper.CardUsageRecordMapper;
import com.washer.backend.mapper.UserCardMapper;
import com.washer.backend.mapper.UserDailyDiscountRecordMapper;
import com.washer.backend.mapper.UserStoreWalletMapper;
import com.washer.backend.mapper.WashOrderMapper;
import com.washer.backend.mapper.WashOrderPaymentDetailMapper;
import com.washer.backend.mapper.WashOrderStatusLogMapper;
import com.washer.backend.mapper.WalletTransactionMapper;
import com.washer.backend.service.DeviceService;
import com.washer.backend.service.StoreService;
import com.washer.backend.service.WashOrderService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
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
    private static final String PAY_MODE_WALLET = "wallet";
    private static final String PAY_MODE_CARD = "card";
    private static final String DISCOUNT_TYPE_FIRST_PERIOD = "first_period_discount";
    private static final String DISCOUNT_SCOPE_USER_STORE_DAY = "user_store_day";
    private static final BigDecimal DEFAULT_FIRST_PERIOD_DISCOUNT_AMOUNT = new BigDecimal("5.00");

    private final StoreService storeService;
    private final DeviceService deviceService;
    private final WashOrderStatusLogMapper washOrderStatusLogMapper;
    private final UserCardMapper userCardMapper;
    private final CardUsageRecordMapper cardUsageRecordMapper;
    private final UserDailyDiscountRecordMapper userDailyDiscountRecordMapper;
    private final UserStoreWalletMapper userStoreWalletMapper;
    private final WalletTransactionMapper walletTransactionMapper;
    private final WashOrderPaymentDetailMapper washOrderPaymentDetailMapper;

    public WashOrderServiceImpl(
        StoreService storeService,
        DeviceService deviceService,
        WashOrderStatusLogMapper washOrderStatusLogMapper,
        UserCardMapper userCardMapper,
        CardUsageRecordMapper cardUsageRecordMapper,
        UserDailyDiscountRecordMapper userDailyDiscountRecordMapper,
        UserStoreWalletMapper userStoreWalletMapper,
        WalletTransactionMapper walletTransactionMapper,
        WashOrderPaymentDetailMapper washOrderPaymentDetailMapper
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
                    order.getEstimatedAmount(),
                    resolveSimpleListAmount(order),
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

        BigDecimal baseAmount = resolveRequestedBaseAmount(request);

        WashOrder order = new WashOrder();
        order.setOrderNo("WO" + UUID.randomUUID().toString().replace("-", "").substring(0, 18));
        order.setUserId(request.getUserId());
        order.setStoreId(request.getStoreId());
        order.setDeviceId(request.getDeviceId());
        order.setOrderSource("miniapp");
        order.setOrderStatus(STATUS_PENDING);
        order.setPayMode(StringUtils.hasText(request.getPayMode()) ? request.getPayMode() : PAY_MODE_WALLET);
        order.setPaymentStatus("unpaid");
        order.setRefundStatusSnapshot("none");
        order.setPricingSnapshotVersion(1);
        order.setCardDeductTimes(0);
        order.setEstimatedAmount(baseAmount);
        order.setFinalAmount(baseAmount);
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
    public WashOrder startOrder(Long id) {
        WashOrder order = getRequiredOrder(id);
        if (STATUS_RUNNING.equals(order.getOrderStatus())) {
            return order;
        }
        if (STATUS_COMPLETED.equals(order.getOrderStatus())) {
            throw new IllegalArgumentException("completed order cannot be started");
        }

        String fromStatus = order.getOrderStatus();
        FirstPeriodDiscountResult discountResult = applyFirstPeriodDiscountOnStart(order);
        LambdaUpdateWrapper<WashOrder> wrapper = new LambdaUpdateWrapper<WashOrder>()
            .eq(WashOrder::getId, order.getId())
            .set(WashOrder::getOrderStatus, STATUS_RUNNING)
            .set(WashOrder::getStartTime, order.getStartTime() != null ? order.getStartTime() : LocalDateTime.now())
            .set(WashOrder::getPaymentStatus, "unpaid")
            .set(WashOrder::getIsFirstPeriodDiscountUsed, discountResult.used() ? 1 : 0)
            .set(WashOrder::getFirstPeriodDiscountAmount, discountResult.discountAmount())
            .set(WashOrder::getFinalAmount, discountResult.finalAmount());
        this.update(wrapper);

        WashOrder updatedOrder = getRequiredOrder(id);
        insertStatusLog(updatedOrder, fromStatus, STATUS_RUNNING, "start", "user", updatedOrder.getUserId(), discountResult.logRemark());
        return updatedOrder;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WashOrder completeOrder(Long id) {
        WashOrder order = repairDiscountFieldsBeforeSettle(getRequiredOrder(id));
        if (STATUS_COMPLETED.equals(order.getOrderStatus())) {
            return order;
        }

        if (PAY_MODE_WALLET.equals(order.getPayMode())) {
            return completeWalletOrder(order);
        }

        if (PAY_MODE_CARD.equals(order.getPayMode())) {
            return completeCardOrder(order);
        }

        throw new IllegalArgumentException("unsupported pay_mode");
    }

    private WashOrder completeWalletOrder(WashOrder order) {
        String fromStatus = order.getOrderStatus();
        BigDecimal finalAmount = normalizeAmount(order.getFinalAmount());
        WalletAllocation allocation = buildWalletAllocation(order, finalAmount);
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
        updateOrderAsPaid(order, allocation.totalAmount(), paymentStatusDesc, null, 0);

        WashOrder updatedOrder = getRequiredOrder(order.getId());
        insertStatusLog(updatedOrder, fromStatus, STATUS_COMPLETED, "finish", "user", updatedOrder.getUserId(), "完成订单");
        return updatedOrder;
    }

    private WashOrder completeCardOrder(WashOrder order) {
        String fromStatus = order.getOrderStatus();
        BigDecimal finalAmount = order.getFinalAmount() != null ? order.getFinalAmount() : BigDecimal.ZERO;
        UserCard userCard = getRequiredAvailableCard(order.getUserId(), order.getStoreId());
        Integer remainingTimes = userCard.getRemainingTimes() != null ? userCard.getRemainingTimes() : 0;
        if (remainingTimes < 1) {
            throw new IllegalArgumentException("user card remaining times is not enough");
        }

        Integer usedTimes = userCard.getUsedTimes() != null ? userCard.getUsedTimes() : 0;
        Integer newRemainingTimes = remainingTimes - 1;
        String newStatus = newRemainingTimes > 0 ? "active" : "used_up";

        LambdaUpdateWrapper<UserCard> cardWrapper = new LambdaUpdateWrapper<UserCard>()
            .eq(UserCard::getId, userCard.getId())
            .set(UserCard::getUsedTimes, usedTimes + 1)
            .set(UserCard::getRemainingTimes, newRemainingTimes)
            .set(UserCard::getStatus, newStatus);
        userCardMapper.update(null, cardWrapper);

        CardUsageRecord usageRecord = insertCardUsageRecord(order, userCard);
        String bizActionNo = "ORDER_PAY_" + order.getOrderNo();
        insertCardPaymentDetail(order, finalAmount, bizActionNo, userCard.getId());

        updateOrderAsPaid(order, finalAmount, "card paid", usageRecord.getId(), 1);

        WashOrder updatedOrder = getRequiredOrder(order.getId());
        insertStatusLog(updatedOrder, fromStatus, STATUS_COMPLETED, "finish", "user", updatedOrder.getUserId(), "完成订单");
        return updatedOrder;
    }

    private void updateOrderAsPaid(
        WashOrder order,
        BigDecimal paidAmount,
        String paymentStatusDesc,
        Long cardUsageId,
        Integer cardDeductTimes
    ) {
        LambdaUpdateWrapper<WashOrder> wrapper = new LambdaUpdateWrapper<WashOrder>()
            .eq(WashOrder::getId, order.getId())
            .set(WashOrder::getOrderStatus, STATUS_COMPLETED)
            .set(WashOrder::getEndTime, LocalDateTime.now())
            .set(WashOrder::getSettleTime, LocalDateTime.now())
            .set(WashOrder::getPaymentStatus, "paid")
            .set(WashOrder::getPaymentStatusDesc, paymentStatusDesc)
            .set(WashOrder::getPaidAmount, paidAmount);
        if (cardUsageId != null) {
            wrapper.set(WashOrder::getCardUsageId, cardUsageId);
        }
        wrapper.set(WashOrder::getCardDeductTimes, cardDeductTimes);
        this.update(wrapper);
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

    private BigDecimal resolveRequestedBaseAmount(SimpleOrderCreateRequest request) {
        if (request.getEstimatedAmount() != null) {
            return normalizeAmount(request.getEstimatedAmount());
        }
        return normalizeAmount(request.getFinalAmount());
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

    private List<UserStoreWallet> getUserWallets(Long userId) {
        return userStoreWalletMapper.selectList(
            new LambdaQueryWrapper<UserStoreWallet>()
                .eq(UserStoreWallet::getUserId, userId)
                .orderByAsc(UserStoreWallet::getStoreId)
                .orderByAsc(UserStoreWallet::getId)
        );
    }

    private UserCard getRequiredAvailableCard(Long userId, Long storeId) {
        UserCard userCard = userCardMapper.selectOne(
            new LambdaQueryWrapper<UserCard>()
                .eq(UserCard::getUserId, userId)
                .eq(UserCard::getStoreId, storeId)
                .eq(UserCard::getStatus, "active")
                .gt(UserCard::getRemainingTimes, 0)
                .orderByAsc(UserCard::getId)
                .last("limit 1")
        );
        if (userCard == null) {
            throw new IllegalArgumentException("available user card not found");
        }
        return userCard;
    }

    private WashOrder getRequiredOrder(Long id) {
        WashOrder order = this.getById(id);
        if (order == null) {
            throw new IllegalArgumentException("order not found");
        }
        return order;
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
