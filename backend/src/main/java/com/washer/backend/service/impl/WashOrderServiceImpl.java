package com.washer.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.washer.backend.dto.order.SimpleOrderCreateRequest;
import com.washer.backend.dto.order.SimpleOrderItem;
import com.washer.backend.entity.Store;
import com.washer.backend.entity.UserStoreWallet;
import com.washer.backend.entity.WashOrder;
import com.washer.backend.entity.WashOrderPaymentDetail;
import com.washer.backend.entity.WashOrderStatusLog;
import com.washer.backend.entity.WalletTransaction;
import com.washer.backend.mapper.UserStoreWalletMapper;
import com.washer.backend.mapper.WashOrderMapper;
import com.washer.backend.mapper.WashOrderPaymentDetailMapper;
import com.washer.backend.mapper.WashOrderStatusLogMapper;
import com.washer.backend.mapper.WalletTransactionMapper;
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
    private static final String PAY_MODE_WALLET = "wallet";

    private final StoreService storeService;
    private final WashOrderStatusLogMapper washOrderStatusLogMapper;
    private final UserStoreWalletMapper userStoreWalletMapper;
    private final WalletTransactionMapper walletTransactionMapper;
    private final WashOrderPaymentDetailMapper washOrderPaymentDetailMapper;

    public WashOrderServiceImpl(
        StoreService storeService,
        WashOrderStatusLogMapper washOrderStatusLogMapper,
        UserStoreWalletMapper userStoreWalletMapper,
        WalletTransactionMapper walletTransactionMapper,
        WashOrderPaymentDetailMapper washOrderPaymentDetailMapper
    ) {
        this.storeService = storeService;
        this.washOrderStatusLogMapper = washOrderStatusLogMapper;
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
        order.setPayMode(StringUtils.hasText(request.getPayMode()) ? request.getPayMode() : PAY_MODE_WALLET);
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
        if (!PAY_MODE_WALLET.equals(order.getPayMode())) {
            throw new IllegalArgumentException("only wallet pay_mode is supported now");
        }

        String fromStatus = order.getOrderStatus();
        BigDecimal finalAmount = order.getFinalAmount() != null ? order.getFinalAmount() : BigDecimal.ZERO;
        UserStoreWallet wallet = getRequiredWallet(order.getUserId(), order.getStoreId());
        if (wallet.getStatus() != null && wallet.getStatus() == 0) {
            throw new IllegalArgumentException("wallet is frozen");
        }

        BigDecimal availablePrincipal = wallet.getAvailablePrincipalBalance() != null
            ? wallet.getAvailablePrincipalBalance()
            : (wallet.getPrincipalBalance() != null ? wallet.getPrincipalBalance() : BigDecimal.ZERO);
        if (availablePrincipal.compareTo(finalAmount) < 0) {
            throw new IllegalArgumentException("wallet principal balance is not enough");
        }

        BigDecimal principalBalance = wallet.getPrincipalBalance() != null
            ? wallet.getPrincipalBalance()
            : availablePrincipal;
        BigDecimal totalConsumePrincipal = wallet.getTotalConsumePrincipal() != null
            ? wallet.getTotalConsumePrincipal()
            : BigDecimal.ZERO;
        BigDecimal balanceAfter = availablePrincipal.subtract(finalAmount);

        LambdaUpdateWrapper<UserStoreWallet> walletWrapper = new LambdaUpdateWrapper<UserStoreWallet>()
            .eq(UserStoreWallet::getId, wallet.getId())
            .set(UserStoreWallet::getPrincipalBalance, principalBalance.subtract(finalAmount))
            .set(UserStoreWallet::getAvailablePrincipalBalance, balanceAfter)
            .set(UserStoreWallet::getTotalConsumePrincipal, totalConsumePrincipal.add(finalAmount));
        userStoreWalletMapper.update(null, walletWrapper);

        String bizActionNo = "ORDER_PAY_" + order.getOrderNo();
        insertWalletTransaction(order, availablePrincipal, balanceAfter, finalAmount, bizActionNo);
        insertOrderPaymentDetail(order, finalAmount, bizActionNo);

        LambdaUpdateWrapper<WashOrder> wrapper = new LambdaUpdateWrapper<WashOrder>()
            .eq(WashOrder::getId, id)
            .set(WashOrder::getOrderStatus, STATUS_COMPLETED)
            .set(WashOrder::getEndTime, LocalDateTime.now())
            .set(WashOrder::getSettleTime, LocalDateTime.now())
            .set(WashOrder::getPaymentStatus, "paid")
            .set(WashOrder::getPaymentStatusDesc, "wallet principal paid")
            .set(WashOrder::getPaidAmount, finalAmount);
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

    private UserStoreWallet getRequiredWallet(Long userId, Long storeId) {
        UserStoreWallet wallet = userStoreWalletMapper.selectOne(
            new LambdaQueryWrapper<UserStoreWallet>()
                .eq(UserStoreWallet::getUserId, userId)
                .eq(UserStoreWallet::getStoreId, storeId)
                .last("limit 1")
        );
        if (wallet == null) {
            throw new IllegalArgumentException("wallet not found");
        }
        return wallet;
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

    private void insertWalletTransaction(
        WashOrder order,
        BigDecimal balanceBefore,
        BigDecimal balanceAfter,
        BigDecimal amount,
        String bizActionNo
    ) {
        WalletTransaction transaction = new WalletTransaction();
        transaction.setTransactionNo("WT" + UUID.randomUUID().toString().replace("-", "").substring(0, 18));
        transaction.setUserId(order.getUserId());
        transaction.setStoreId(order.getStoreId());
        transaction.setBizType("consume");
        transaction.setAmountType("principal");
        transaction.setBalanceBucket("available");
        transaction.setChangeType("out");
        transaction.setAmount(amount);
        transaction.setRelatedAction("consume");
        transaction.setBizActionNo(bizActionNo);
        transaction.setBalanceBefore(balanceBefore);
        transaction.setBalanceAfter(balanceAfter);
        transaction.setRelatedOrderId(order.getId());
        transaction.setRelatedOrderNo(order.getOrderNo());
        transaction.setRemark("wallet principal pay for wash order");
        walletTransactionMapper.insert(transaction);
    }

    private void insertOrderPaymentDetail(WashOrder order, BigDecimal amount, String bizActionNo) {
        WashOrderPaymentDetail detail = new WashOrderPaymentDetail();
        detail.setOrderId(order.getId());
        detail.setOrderNo(order.getOrderNo());
        detail.setUserId(order.getUserId());
        detail.setConsumeStoreId(order.getStoreId());
        detail.setSourceType("wallet");
        detail.setSourceStoreId(order.getStoreId());
        detail.setAmountType("principal");
        detail.setAmount(amount);
        detail.setDeductTimes(0);
        detail.setPaymentSeq(1);
        detail.setSettleStage("final");
        detail.setAllocationStrategy("manual");
        detail.setBizActionNo(bizActionNo);
        detail.setRefundedAmount(BigDecimal.ZERO);
        washOrderPaymentDetailMapper.insert(detail);
    }
}
