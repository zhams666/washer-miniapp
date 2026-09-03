package com.washer.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.washer.backend.dto.admin.AdminWalletRechargeRequest;
import com.washer.backend.dto.admin.AdminWalletRechargeResult;
import com.washer.backend.dto.pay.WechatPayNotifyResult;
import com.washer.backend.dto.pay.WechatPayOrderQueryResult;
import com.washer.backend.dto.pay.WechatPayPrepayRequest;
import com.washer.backend.dto.pay.WechatPayPrepayResult;
import com.washer.backend.entity.PaymentCallbackLog;
import com.washer.backend.entity.PaymentTransaction;
import com.washer.backend.entity.RechargeOrder;
import com.washer.backend.entity.Store;
import com.washer.backend.entity.UserInfo;
import com.washer.backend.entity.UserStoreWallet;
import com.washer.backend.entity.WalletTransaction;
import com.washer.backend.entity.WalletRechargeProduct;
import com.washer.backend.mapper.PaymentCallbackLogMapper;
import com.washer.backend.mapper.PaymentTransactionMapper;
import com.washer.backend.mapper.RechargeOrderMapper;
import com.washer.backend.mapper.UserStoreWalletMapper;
import com.washer.backend.mapper.WalletTransactionMapper;
import com.washer.backend.mapper.WalletRechargeProductMapper;
import com.washer.backend.service.AdminWalletRechargeService;
import com.washer.backend.service.MembershipService;
import com.washer.backend.service.StoreService;
import com.washer.backend.service.UserInfoService;
import com.washer.backend.service.WechatPayService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AdminWalletRechargeServiceImpl implements AdminWalletRechargeService {

    private final UserInfoService userInfoService;
    private final StoreService storeService;
    private final UserStoreWalletMapper userStoreWalletMapper;
    private final WalletTransactionMapper walletTransactionMapper;
    private final WalletRechargeProductMapper walletRechargeProductMapper;
    private final RechargeOrderMapper rechargeOrderMapper;
    private final PaymentTransactionMapper paymentTransactionMapper;
    private final PaymentCallbackLogMapper paymentCallbackLogMapper;
    private final WechatPayService wechatPayService;
    private final MembershipService membershipService;

    public AdminWalletRechargeServiceImpl(
        UserInfoService userInfoService,
        StoreService storeService,
        UserStoreWalletMapper userStoreWalletMapper,
        WalletTransactionMapper walletTransactionMapper,
        WalletRechargeProductMapper walletRechargeProductMapper,
        RechargeOrderMapper rechargeOrderMapper,
        PaymentTransactionMapper paymentTransactionMapper,
        PaymentCallbackLogMapper paymentCallbackLogMapper,
        WechatPayService wechatPayService,
        MembershipService membershipService
    ) {
        this.userInfoService = userInfoService;
        this.storeService = storeService;
        this.userStoreWalletMapper = userStoreWalletMapper;
        this.walletTransactionMapper = walletTransactionMapper;
        this.walletRechargeProductMapper = walletRechargeProductMapper;
        this.rechargeOrderMapper = rechargeOrderMapper;
        this.paymentTransactionMapper = paymentTransactionMapper;
        this.paymentCallbackLogMapper = paymentCallbackLogMapper;
        this.wechatPayService = wechatPayService;
        this.membershipService = membershipService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminWalletRechargeResult manualRecharge(AdminWalletRechargeRequest request) {
        RechargeExecutionContext context = prepareRecharge(request, false);
        RechargeOrder rechargeOrder = insertRechargeOrder(
            request,
            context.principalAmount,
            context.giftAmount,
            context.payAmount,
            "manual",
            "paid",
            LocalDateTime.now()
        );
        WalletRechargeResult walletResult = applyRecharge(
            context,
            "RECHARGE_" + rechargeOrder.getRechargeOrderNo(),
            "manual"
        );
        ensureUserMembership(context.request.getUserId());
        return buildRechargeResult(rechargeOrder, null, walletResult.walletId, "paid", null, null, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminWalletRechargeResult createMiniappRechargeOrder(AdminWalletRechargeRequest request) {
        RechargeExecutionContext context = prepareMiniappRecharge(request);
        if (wechatPayService.isEnabled()) {
            return createWechatPayRechargeOrder(context);
        }

        RechargeOrder rechargeOrder = insertRechargeOrder(
            request,
            context.principalAmount,
            context.giftAmount,
            context.payAmount,
            "miniapp_mock",
            "pending",
            null
        );
        WalletRechargeResult walletResult = applyRecharge(
            context,
            "RECHARGE_" + rechargeOrder.getRechargeOrderNo(),
            "miniapp mock"
        );
        ensureUserMembership(context.request.getUserId());
        markRechargeOrderPaid(rechargeOrder.getId(), "miniapp_mock");
        rechargeOrder.setPayStatus("paid");
        rechargeOrder.setPayChannel("miniapp_mock");
        rechargeOrder.setPayTime(LocalDateTime.now());
        return buildRechargeResult(rechargeOrder, null, walletResult.walletId, "paid", null, null, null);
    }

    @Override
    public AdminWalletRechargeResult getRechargeOrderResult(String rechargeOrderNo) {
        if (!StringUtils.hasText(rechargeOrderNo)) {
            throw new IllegalArgumentException("rechargeOrderNo is required");
        }

        RechargeOrder rechargeOrder = rechargeOrderMapper.selectOne(
            new LambdaQueryWrapper<RechargeOrder>()
                .eq(RechargeOrder::getRechargeOrderNo, rechargeOrderNo.trim())
                .last("limit 1")
        );
        if (rechargeOrder == null) {
            throw new IllegalArgumentException("recharge order not found");
        }

        Long walletId = findWalletId(rechargeOrder.getUserId(), rechargeOrder.getStoreId());
        PaymentTransaction payment = findPaymentByRechargeOrderNo(rechargeOrder.getRechargeOrderNo());
        return buildRechargeResult(
            rechargeOrder,
            payment,
            walletId,
            rechargeOrder.getPayStatus(),
            null,
            payment != null ? payment.getExpireTime() : null,
            resolvePaymentFailReason(payment)
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminWalletRechargeResult syncRechargeOrder(String rechargeOrderNo) {
        RechargeOrder rechargeOrder = getRequiredRechargeOrder(rechargeOrderNo);
        PaymentTransaction payment = findPaymentByRechargeOrderNo(rechargeOrder.getRechargeOrderNo());
        if (payment == null || !"pending".equals(payment.getPayStatus()) || !wechatPayService.isEnabled()) {
            return buildRechargeResult(
                rechargeOrder,
                payment,
                findWalletId(rechargeOrder.getUserId(), rechargeOrder.getStoreId()),
                rechargeOrder.getPayStatus(),
                null,
                payment != null ? payment.getExpireTime() : null,
                resolvePaymentFailReason(payment)
            );
        }

        WechatPayOrderQueryResult queryResult = wechatPayService.queryOrderByOutTradeNo(payment.getOutTradeNo());
        if ("SUCCESS".equals(queryResult.getTradeState())) {
            confirmWechatPaySuccess(
                payment,
                queryResult.getTransactionId(),
                queryResult.getPayerTotal(),
                queryResult.getSuccessTime(),
                "sync"
            );
            rechargeOrder = getRequiredRechargeOrder(rechargeOrderNo);
            payment = findPaymentByRechargeOrderNo(rechargeOrder.getRechargeOrderNo());
        } else if ("CLOSED".equals(queryResult.getTradeState()) || "REVOKED".equals(queryResult.getTradeState())) {
            markPaymentTerminal(payment, "closed", queryResult.getTradeState());
            markRechargeOrderTerminal(rechargeOrder.getId(), "closed", queryResult.getTradeState());
            rechargeOrder.setPayStatus("closed");
            payment.setPayStatus("closed");
            payment.setFailReason(queryResult.getTradeState());
        } else if ("PAYERROR".equals(queryResult.getTradeState())) {
            markPaymentTerminal(payment, "failed", queryResult.getTradeState());
            markRechargeOrderTerminal(rechargeOrder.getId(), "failed", queryResult.getTradeState());
            rechargeOrder.setPayStatus("failed");
            payment.setPayStatus("failed");
            payment.setFailReason(queryResult.getTradeState());
        }

        return buildRechargeResult(
            rechargeOrder,
            payment,
            findWalletId(rechargeOrder.getUserId(), rechargeOrder.getStoreId()),
            rechargeOrder.getPayStatus(),
            null,
            payment != null ? payment.getExpireTime() : null,
            resolvePaymentFailReason(payment)
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, String> handleWechatPayNotify(Map<String, String> headers, String body) {
        try {
            WechatPayNotifyResult notify = wechatPayService.parseNotify(headers, body);
            PaymentTransaction payment = findPaymentByOutTradeNo(notify.getOutTradeNo());
            if (payment == null) {
                insertCallbackLog(null, notify, 1, "failed", "payment transaction not found");
                return Map.of("code", "FAIL", "message", "payment transaction not found");
            }

            if ("TRANSACTION.SUCCESS".equals(notify.getEventType()) && "SUCCESS".equals(notify.getTradeState())) {
                confirmWechatPaySuccess(
                    payment,
                    notify.getTransactionId(),
                    notify.getPayerTotal(),
                    notify.getSuccessTime(),
                    "notify"
                );
                insertCallbackLog(payment, notify, 1, "success", "paid");
            } else {
                insertCallbackLog(payment, notify, 1, "ignored", notify.getTradeState());
            }
            return Map.of("code", "SUCCESS", "message", "成功");
        } catch (Exception ex) {
            PaymentCallbackLog log = new PaymentCallbackLog();
            log.setCallbackNo("PCB" + UUID.randomUUID().toString().replace("-", "").substring(0, 18));
            log.setPayChannel("wxpay");
            log.setCallbackType("payment_notify");
            log.setSignVerified(0);
            log.setProcessStatus("failed");
            log.setProcessResult(ex.getMessage());
            log.setRawContent(body);
            paymentCallbackLogMapper.insert(log);
            return Map.of("code", "FAIL", "message", ex.getMessage() == null ? "notify failed" : ex.getMessage());
        }
    }

    private RechargeExecutionContext prepareRecharge(
        AdminWalletRechargeRequest request,
        boolean requirePositivePrincipal
    ) {
        if (request == null) {
            throw new IllegalArgumentException("request is required");
        }
        if (request.getUserId() == null) {
            throw new IllegalArgumentException("userId is required");
        }
        if (request.getStoreId() == null) {
            throw new IllegalArgumentException("storeId is required");
        }

        BigDecimal principalAmount = normalizeAmount(request.getPrincipalAmount());
        BigDecimal giftAmount = normalizeAmount(request.getGiftAmount());
        if (principalAmount.compareTo(BigDecimal.ZERO) < 0 || giftAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("recharge amount must be >= 0");
        }
        if (requirePositivePrincipal && principalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("principalAmount is required");
        }
        if (!requirePositivePrincipal
            && principalAmount.compareTo(BigDecimal.ZERO) <= 0
            && giftAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("principal or gift amount is required");
        }

        UserInfo user = userInfoService.getById(request.getUserId());
        if (user == null) {
            throw new IllegalArgumentException("user not found");
        }
        Store store = storeService.getById(request.getStoreId());
        if (store == null) {
            throw new IllegalArgumentException("store not found");
        }

        BigDecimal payAmount = normalizeAmount(request.getPayAmount());
        if (payAmount.compareTo(BigDecimal.ZERO) <= 0) {
            payAmount = principalAmount;
        }

        UserStoreWallet wallet = loadOrCreateWalletForUpdate(request.getUserId(), request.getStoreId());
        return new RechargeExecutionContext(request, principalAmount, giftAmount, payAmount, wallet);
    }

    private RechargeExecutionContext prepareMiniappRecharge(AdminWalletRechargeRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request is required");
        }
        if (request.getUserId() == null) {
            throw new IllegalArgumentException("userId is required");
        }
        if (request.getStoreId() == null) {
            throw new IllegalArgumentException("storeId is required");
        }
        if (request.getRechargeProductId() == null) {
            throw new IllegalArgumentException("rechargeProductId is required");
        }

        UserInfo user = userInfoService.getById(request.getUserId());
        if (user == null) {
            throw new IllegalArgumentException("user not found");
        }
        Store store = storeService.getById(request.getStoreId());
        if (store == null) {
            throw new IllegalArgumentException("store not found");
        }

        WalletRechargeProduct product = getRequiredActiveRechargeProduct(
            request.getRechargeProductId(),
            request.getStoreId()
        );
        ensureRechargeProductPurchaseAllowed(product, request.getUserId());

        request.setPayAmount(product.getPayAmount());
        request.setPrincipalAmount(product.getPrincipalAmount());
        request.setGiftAmount(product.getGiftAmount());
        request.setRechargeProductId(product.getId());

        UserStoreWallet wallet = loadOrCreateWalletForUpdate(request.getUserId(), request.getStoreId());
        return new RechargeExecutionContext(
            request,
            product.getPrincipalAmount(),
            product.getGiftAmount(),
            product.getPayAmount(),
            wallet
        );
    }

    private RechargeExecutionContext preparePaidRechargeFromOrder(RechargeOrder rechargeOrder) {
        if (rechargeOrder == null) {
            throw new IllegalArgumentException("recharge order is required");
        }

        AdminWalletRechargeRequest request = new AdminWalletRechargeRequest();
        request.setUserId(rechargeOrder.getUserId());
        request.setStoreId(rechargeOrder.getStoreId());
        request.setRechargeProductId(rechargeOrder.getRechargeProductId());
        request.setPayAmount(rechargeOrder.getPayAmount());
        request.setPrincipalAmount(rechargeOrder.getPrincipalAmount());
        request.setGiftAmount(rechargeOrder.getGiftAmount());
        request.setRemark("wechat pay recharge");

        UserInfo user = userInfoService.getById(rechargeOrder.getUserId());
        if (user == null) {
            throw new IllegalArgumentException("user not found");
        }
        Store store = storeService.getById(rechargeOrder.getStoreId());
        if (store == null) {
            throw new IllegalArgumentException("store not found");
        }

        UserStoreWallet wallet = loadOrCreateWalletForUpdate(rechargeOrder.getUserId(), rechargeOrder.getStoreId());
        return new RechargeExecutionContext(
            request,
            normalizeAmount(rechargeOrder.getPrincipalAmount()),
            normalizeAmount(rechargeOrder.getGiftAmount()),
            normalizeAmount(rechargeOrder.getPayAmount()),
            wallet
        );
    }

    private WalletRechargeProduct getRequiredActiveRechargeProduct(Long rechargeProductId, Long storeId) {
        LocalDateTime now = LocalDateTime.now();
        WalletRechargeProduct product = walletRechargeProductMapper.selectOne(
            new LambdaQueryWrapper<WalletRechargeProduct>()
                .eq(WalletRechargeProduct::getId, rechargeProductId)
                .eq(WalletRechargeProduct::getStoreId, storeId)
                .eq(WalletRechargeProduct::getStatus, 1)
                .last("limit 1")
        );
        if (!isRechargeProductActive(product, now)) {
            throw new IllegalArgumentException("recharge product not found or off shelf");
        }
        validateRechargeProductSnapshot(product);
        return product;
    }

    private void validateRechargeProductSnapshot(WalletRechargeProduct product) {
        if (normalizeAmount(product.getPayAmount()).compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("recharge product payAmount is invalid");
        }
        if (normalizeAmount(product.getPrincipalAmount()).compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("recharge product principalAmount is invalid");
        }
        if (normalizeAmount(product.getGiftAmount()).compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("recharge product giftAmount is invalid");
        }
    }

    private void ensureRechargeProductPurchaseAllowed(WalletRechargeProduct product, Long userId) {
        Integer purchaseLimit = product.getPurchaseLimit();
        if (purchaseLimit == null || purchaseLimit <= 0) {
            return;
        }
        Long paidCount = rechargeOrderMapper.selectCount(
            new LambdaQueryWrapper<RechargeOrder>()
                .eq(RechargeOrder::getUserId, userId)
                .eq(RechargeOrder::getStoreId, product.getStoreId())
                .eq(RechargeOrder::getRechargeProductId, product.getId())
                .in(RechargeOrder::getPayStatus, "pending", "paid")
        );
        if (paidCount != null && paidCount >= purchaseLimit) {
            throw new IllegalArgumentException("recharge product purchase limit exceeded");
        }
    }

    private boolean isRechargeProductActive(WalletRechargeProduct product, LocalDateTime now) {
        if (product == null || !Integer.valueOf(1).equals(product.getStatus())) {
            return false;
        }
        if (product.getEffectiveTime() != null && product.getEffectiveTime().isAfter(now)) {
            return false;
        }
        return product.getExpireTime() == null || product.getExpireTime().isAfter(now);
    }

    private UserStoreWallet loadOrCreateWalletForUpdate(Long userId, Long storeId) {
        UserStoreWallet wallet = userStoreWalletMapper.selectOne(
            new LambdaQueryWrapper<UserStoreWallet>()
                .eq(UserStoreWallet::getUserId, userId)
                .eq(UserStoreWallet::getStoreId, storeId)
                .last("limit 1")
        );

        if (wallet == null) {
            wallet = buildEmptyWallet(userId, storeId);
            try {
                userStoreWalletMapper.insert(wallet);
            } catch (DuplicateKeyException ex) {
                wallet = userStoreWalletMapper.selectOne(
                    new LambdaQueryWrapper<UserStoreWallet>()
                        .eq(UserStoreWallet::getUserId, userId)
                        .eq(UserStoreWallet::getStoreId, storeId)
                        .last("limit 1")
                );
            }
        }

        return wallet;
    }

    private WalletRechargeResult applyRecharge(
        RechargeExecutionContext context,
        String bizActionNo,
        String remarkSource
    ) {
        UserStoreWallet wallet = context.wallet;
        if (wallet == null || wallet.getId() == null) {
            throw new IllegalStateException("wallet not found");
        }
        BigDecimal principalAmount = context.principalAmount;
        BigDecimal giftAmount = context.giftAmount;

        if (hasWalletTransaction(bizActionNo)) {
            return new WalletRechargeResult(wallet.getId());
        }

        BigDecimal principalBalanceBefore = resolvePrincipalBalance(wallet);
        BigDecimal principalAvailableBefore = resolveAvailablePrincipal(wallet);
        BigDecimal giftBalanceBefore = resolveGiftBalance(wallet);
        BigDecimal giftAvailableBefore = resolveAvailableGift(wallet);

        BigDecimal principalBalanceAfter = principalBalanceBefore.add(principalAmount);
        BigDecimal principalAvailableAfter = principalAvailableBefore.add(principalAmount);
        BigDecimal giftBalanceAfter = giftBalanceBefore.add(giftAmount);
        BigDecimal giftAvailableAfter = giftAvailableBefore.add(giftAmount);
        BigDecimal totalRechargePrincipal = resolveAmount(wallet.getTotalRechargePrincipal()).add(principalAmount);
        BigDecimal totalRechargeGift = resolveAmount(wallet.getTotalRechargeGift()).add(giftAmount);

        LambdaUpdateWrapper<UserStoreWallet> walletWrapper = new LambdaUpdateWrapper<UserStoreWallet>()
            .eq(UserStoreWallet::getId, wallet.getId())
            .set(UserStoreWallet::getPrincipalBalance, principalBalanceAfter)
            .set(UserStoreWallet::getAvailablePrincipalBalance, principalAvailableAfter)
            .set(UserStoreWallet::getGiftBalance, giftBalanceAfter)
            .set(UserStoreWallet::getAvailableGiftBalance, giftAvailableAfter)
            .set(UserStoreWallet::getTotalRechargePrincipal, totalRechargePrincipal)
            .set(UserStoreWallet::getTotalRechargeGift, totalRechargeGift);
        int updatedWalletRows = userStoreWalletMapper.update(null, walletWrapper);
        if (updatedWalletRows <= 0) {
            throw new IllegalStateException("wallet changed while applying recharge");
        }

        if (principalAmount.compareTo(BigDecimal.ZERO) > 0) {
            insertWalletTransaction(
                context.request.getUserId(),
                context.request.getStoreId(),
                "principal",
                principalAvailableBefore,
                principalAvailableAfter,
                principalAmount,
                bizActionNo,
                context.request.getRemark(),
                remarkSource
            );
        }

        if (giftAmount.compareTo(BigDecimal.ZERO) > 0) {
            insertWalletTransaction(
                context.request.getUserId(),
                context.request.getStoreId(),
                "gift",
                giftAvailableBefore,
                giftAvailableAfter,
                giftAmount,
                bizActionNo,
                context.request.getRemark(),
                remarkSource
            );
        }

        creditRechargePoints(context.request.getUserId(), context.payAmount);
        return new WalletRechargeResult(wallet.getId());
    }

    private void creditRechargePoints(Long userId, BigDecimal payAmount) {
        if (userId == null) {
            return;
        }
        BigDecimal pointAmount = normalizeAmount(payAmount)
            .multiply(new BigDecimal("0.10"))
            .setScale(0, RoundingMode.DOWN);
        int points = pointAmount.intValue();
        if (points <= 0) {
            return;
        }

        UserInfo user = userInfoService.getById(userId);
        if (user == null) {
            throw new IllegalArgumentException("user not found");
        }
        int currentPoints = user.getPoints() != null ? user.getPoints() : 0;
        boolean updated = userInfoService.update(
            null,
            new LambdaUpdateWrapper<UserInfo>()
                .eq(UserInfo::getId, userId)
                .set(UserInfo::getPoints, currentPoints + points)
                .set(UserInfo::getUpdatedAt, LocalDateTime.now())
        );
        if (!updated) {
            throw new IllegalStateException("recharge points update failed");
        }
    }

    private AdminWalletRechargeResult createWechatPayRechargeOrder(RechargeExecutionContext context) {
        RechargeOrder rechargeOrder = insertRechargeOrder(
            context.request,
            context.principalAmount,
            context.giftAmount,
            context.payAmount,
            "wxpay",
            "pending",
            null
        );
        PaymentTransaction payment = insertPaymentTransaction(rechargeOrder, context.payAmount);
        UserInfo user = userInfoService.getById(context.request.getUserId());
        if (user == null || !StringUtils.hasText(user.getOpenid())) {
            throw new IllegalArgumentException("user openid is required for wechat pay");
        }

        WechatPayPrepayResult prepay = wechatPayService.createJsapiPrepay(
            new WechatPayPrepayRequest(
                "自助洗车余额充值",
                payment.getOutTradeNo(),
                context.payAmount,
                user.getOpenid(),
                rechargeOrder.getRechargeOrderNo()
            )
        );
        paymentTransactionMapper.update(
            null,
            new LambdaUpdateWrapper<PaymentTransaction>()
                .eq(PaymentTransaction::getId, payment.getId())
                .set(PaymentTransaction::getPrepayId, prepay.getPrepayId())
        );
        payment.setPrepayId(prepay.getPrepayId());
        return buildRechargeResult(
            rechargeOrder,
            payment,
            null,
            "pending",
            prepay.getPayParams(),
            payment.getExpireTime(),
            null
        );
    }

    private RechargeOrder insertRechargeOrder(
        AdminWalletRechargeRequest request,
        BigDecimal principalAmount,
        BigDecimal giftAmount,
        BigDecimal payAmount,
        String payChannel,
        String payStatus,
        LocalDateTime payTime
    ) {
        RechargeOrder rechargeOrder = new RechargeOrder();
        rechargeOrder.setRechargeOrderNo("RC" + UUID.randomUUID().toString().replace("-", "").substring(0, 18));
        rechargeOrder.setUserId(request.getUserId());
        rechargeOrder.setStoreId(request.getStoreId());
        rechargeOrder.setRechargeProductId(request.getRechargeProductId());
        rechargeOrder.setPrincipalAmount(principalAmount);
        rechargeOrder.setGiftAmount(giftAmount);
        rechargeOrder.setPayAmount(payAmount);
        rechargeOrder.setPayChannel(payChannel);
        rechargeOrder.setPayStatus(payStatus);
        rechargeOrder.setPayTime(payTime);
        rechargeOrder.setRemark(request.getRemark());
        rechargeOrderMapper.insert(rechargeOrder);
        return rechargeOrder;
    }

    private void markRechargeOrderPaid(Long rechargeOrderId, String payChannel) {
        if (rechargeOrderId == null) {
            throw new IllegalArgumentException("rechargeOrderId is required");
        }
        LambdaUpdateWrapper<RechargeOrder> orderWrapper = new LambdaUpdateWrapper<RechargeOrder>()
            .eq(RechargeOrder::getId, rechargeOrderId)
            .set(RechargeOrder::getPayStatus, "paid")
            .set(RechargeOrder::getPayChannel, payChannel)
            .set(RechargeOrder::getPayTime, LocalDateTime.now());
        rechargeOrderMapper.update(null, orderWrapper);
    }

    private void markRechargeOrderTerminal(Long rechargeOrderId, String payStatus, String reason) {
        if (rechargeOrderId == null) {
            return;
        }
        rechargeOrderMapper.update(
            null,
            new LambdaUpdateWrapper<RechargeOrder>()
                .eq(RechargeOrder::getId, rechargeOrderId)
                .eq(RechargeOrder::getPayStatus, "pending")
                .set(RechargeOrder::getPayStatus, payStatus)
                .set(RechargeOrder::getRemark, "wechat pay " + payStatus + ": " + reason)
        );
    }

    private PaymentTransaction insertPaymentTransaction(RechargeOrder rechargeOrder, BigDecimal payAmount) {
        PaymentTransaction payment = new PaymentTransaction();
        String paymentNo = "PT" + UUID.randomUUID().toString().replace("-", "").substring(0, 18);
        payment.setPaymentNo(paymentNo);
        payment.setRequestNo(paymentNo);
        payment.setBizType("recharge");
        payment.setBizOrderId(rechargeOrder.getId());
        payment.setBizOrderNo(rechargeOrder.getRechargeOrderNo());
        payment.setBizActionNo("RECHARGE_" + rechargeOrder.getRechargeOrderNo());
        payment.setUserId(rechargeOrder.getUserId());
        payment.setStoreId(rechargeOrder.getStoreId());
        payment.setPayChannel("wxpay");
        payment.setOutTradeNo(paymentNo);
        payment.setIdempotencyKey(paymentNo);
        payment.setPayStatus("pending");
        payment.setPayAmount(payAmount);
        payment.setCurrencyCode("CNY");
        payment.setCallbackCount(0);
        payment.setRequestTime(LocalDateTime.now());
        payment.setExpireTime(LocalDateTime.now().plusMinutes(15));
        payment.setRemark("wechat pay recharge pending");
        paymentTransactionMapper.insert(payment);
        return payment;
    }

    private void confirmWechatPaySuccess(
        PaymentTransaction payment,
        String channelTradeNo,
        Integer payerTotal,
        LocalDateTime payTime,
        String source
    ) {
        if (payment == null || payment.getId() == null) {
            throw new IllegalArgumentException("payment transaction is required");
        }
        if ("membership".equals(payment.getBizType())) {
            membershipService.confirmPaymentSuccess(payment, channelTradeNo, payerTotal, payTime, source);
            return;
        }
        if (!amountMatches(payment.getPayAmount(), payerTotal)) {
            throw new IllegalArgumentException("wechat pay amount mismatch");
        }

        int updatedPayment = paymentTransactionMapper.update(
            null,
            new LambdaUpdateWrapper<PaymentTransaction>()
                .eq(PaymentTransaction::getId, payment.getId())
                .eq(PaymentTransaction::getPayStatus, "pending")
                .set(PaymentTransaction::getPayStatus, "paid")
                .set(PaymentTransaction::getChannelTradeNo, channelTradeNo)
                .set(PaymentTransaction::getPayTime, payTime != null ? payTime : LocalDateTime.now())
                .set(PaymentTransaction::getCallbackTime, LocalDateTime.now())
                .set(PaymentTransaction::getRemark, "wechat pay confirmed by " + source)
        );

        if (updatedPayment == 0) {
            return;
        }

        RechargeOrder rechargeOrder = getRequiredRechargeOrder(payment.getBizOrderNo());
        int updatedOrder = rechargeOrderMapper.update(
            null,
            new LambdaUpdateWrapper<RechargeOrder>()
                .eq(RechargeOrder::getId, rechargeOrder.getId())
                .eq(RechargeOrder::getPayStatus, "pending")
                .set(RechargeOrder::getPayStatus, "paid")
                .set(RechargeOrder::getPayChannel, "wxpay")
                .set(RechargeOrder::getPayTime, payTime != null ? payTime : LocalDateTime.now())
                .set(RechargeOrder::getThirdPartyTradeNo, channelTradeNo)
        );

        if (updatedOrder == 0) {
            return;
        }

        RechargeExecutionContext context = preparePaidRechargeFromOrder(rechargeOrder);
        applyRecharge(context, "RECHARGE_" + rechargeOrder.getRechargeOrderNo(), "wechat pay");
        ensureUserMembership(context.request.getUserId());
    }

    private void ensureUserMembership(Long userId) {
        if (userId == null) {
            return;
        }
        UserInfo user = userInfoService.getById(userId);
        if (user == null || Integer.valueOf(1).equals(user.getIsMember())) {
            return;
        }
        user.setIsMember(1);
        user.setMemberLevel(StringUtils.hasText(user.getMemberLevel()) ? user.getMemberLevel() : "normal");
        user.setMemberSinceTime(user.getMemberSinceTime() != null ? user.getMemberSinceTime() : LocalDateTime.now());
        userInfoService.updateById(user);
    }

    private void markPaymentTerminal(PaymentTransaction payment, String payStatus, String reason) {
        if (payment == null || payment.getId() == null) {
            return;
        }
        paymentTransactionMapper.update(
            null,
            new LambdaUpdateWrapper<PaymentTransaction>()
                .eq(PaymentTransaction::getId, payment.getId())
                .eq(PaymentTransaction::getPayStatus, "pending")
                .set(PaymentTransaction::getPayStatus, payStatus)
                .set(PaymentTransaction::getFailReason, reason)
                .set(PaymentTransaction::getRemark, "wechat pay " + payStatus + ": " + reason)
        );
    }

    private void insertWalletTransaction(
        Long userId,
        Long storeId,
        String amountType,
        BigDecimal balanceBefore,
        BigDecimal balanceAfter,
        BigDecimal amount,
        String bizActionNo,
        String remark,
        String remarkSource
    ) {
        if (hasWalletTransaction(bizActionNo, amountType)) {
            return;
        }
        WalletTransaction transaction = new WalletTransaction();
        transaction.setTransactionNo("WT" + UUID.randomUUID().toString().replace("-", "").substring(0, 18));
        transaction.setUserId(userId);
        transaction.setStoreId(storeId);
        transaction.setBizType("recharge");
        transaction.setAmountType(amountType);
        transaction.setBalanceBucket("available");
        transaction.setChangeType("in");
        transaction.setAmount(amount);
        transaction.setRelatedAction("recharge");
        transaction.setBizActionNo(bizActionNo);
        transaction.setBalanceBefore(balanceBefore);
        transaction.setBalanceAfter(balanceAfter);
        transaction.setRemark(buildRechargeRemark(remarkSource, amountType, remark));
        walletTransactionMapper.insert(transaction);
    }

    private boolean hasWalletTransaction(String bizActionNo, String amountType) {
        if (!StringUtils.hasText(bizActionNo) || !StringUtils.hasText(amountType)) {
            return false;
        }
        Long count = walletTransactionMapper.selectCount(
            new LambdaQueryWrapper<WalletTransaction>()
                .eq(WalletTransaction::getBizActionNo, bizActionNo)
                .eq(WalletTransaction::getAmountType, amountType)
        );
        return count != null && count > 0;
    }

    private boolean hasWalletTransaction(String bizActionNo) {
        if (!StringUtils.hasText(bizActionNo)) {
            return false;
        }
        Long count = walletTransactionMapper.selectCount(
            new LambdaQueryWrapper<WalletTransaction>()
                .eq(WalletTransaction::getBizActionNo, bizActionNo)
        );
        return count != null && count > 0;
    }

    private Long findWalletId(Long userId, Long storeId) {
        if (userId == null || storeId == null) {
            return null;
        }
        UserStoreWallet wallet = userStoreWalletMapper.selectOne(
            new LambdaQueryWrapper<UserStoreWallet>()
                .eq(UserStoreWallet::getUserId, userId)
                .eq(UserStoreWallet::getStoreId, storeId)
                .last("limit 1")
        );
        return wallet != null ? wallet.getId() : null;
    }

    private RechargeOrder getRequiredRechargeOrder(String rechargeOrderNo) {
        if (!StringUtils.hasText(rechargeOrderNo)) {
            throw new IllegalArgumentException("rechargeOrderNo is required");
        }
        RechargeOrder rechargeOrder = rechargeOrderMapper.selectOne(
            new LambdaQueryWrapper<RechargeOrder>()
                .eq(RechargeOrder::getRechargeOrderNo, rechargeOrderNo.trim())
                .last("limit 1")
        );
        if (rechargeOrder == null) {
            throw new IllegalArgumentException("recharge order not found");
        }
        return rechargeOrder;
    }

    private PaymentTransaction findPaymentByRechargeOrderNo(String rechargeOrderNo) {
        if (!StringUtils.hasText(rechargeOrderNo)) {
            return null;
        }
        return paymentTransactionMapper.selectOne(
            new LambdaQueryWrapper<PaymentTransaction>()
                .eq(PaymentTransaction::getBizOrderNo, rechargeOrderNo.trim())
                .orderByDesc(PaymentTransaction::getId)
                .last("limit 1")
        );
    }

    private PaymentTransaction findPaymentByOutTradeNo(String outTradeNo) {
        if (!StringUtils.hasText(outTradeNo)) {
            return null;
        }
        return paymentTransactionMapper.selectOne(
            new LambdaQueryWrapper<PaymentTransaction>()
                .eq(PaymentTransaction::getOutTradeNo, outTradeNo.trim())
                .last("limit 1")
        );
    }

    private void insertCallbackLog(
        PaymentTransaction payment,
        WechatPayNotifyResult notify,
        int signVerified,
        String processStatus,
        String processResult
    ) {
        PaymentCallbackLog log = new PaymentCallbackLog();
        log.setPaymentTransactionId(payment != null ? payment.getId() : null);
        log.setPaymentNo(payment != null ? payment.getPaymentNo() : null);
        log.setCallbackNo("PCB" + UUID.randomUUID().toString().replace("-", "").substring(0, 18));
        log.setBizOrderNo(payment != null ? payment.getBizOrderNo() : null);
        log.setPayChannel("wxpay");
        log.setCallbackType("payment_notify");
        log.setChannelTradeNo(notify != null ? notify.getTransactionId() : null);
        log.setIdempotencyKey(notify != null ? notify.getNotifyId() : null);
        log.setNotifyTime(LocalDateTime.now());
        log.setSignVerified(signVerified);
        log.setProcessStatus(processStatus);
        log.setProcessResult(processResult);
        log.setRawContent(notify != null ? notify.getRawContent() : null);
        paymentCallbackLogMapper.insert(log);
    }

    private boolean amountMatches(BigDecimal payAmount, Integer payerTotal) {
        if (payAmount == null || payerTotal == null) {
            return false;
        }
        int expectedFen = payAmount.multiply(new BigDecimal("100"))
            .setScale(0, RoundingMode.HALF_UP)
            .intValueExact();
        return expectedFen == payerTotal;
    }

    private String resolvePaymentFailReason(PaymentTransaction payment) {
        if (payment == null) {
            return null;
        }
        if (StringUtils.hasText(payment.getFailReason())) {
            return payment.getFailReason();
        }
        return payment.getRemark();
    }

    private AdminWalletRechargeResult buildRechargeResult(
        RechargeOrder rechargeOrder,
        PaymentTransaction payment,
        Long walletId,
        String payStatus,
        com.washer.backend.dto.pay.WxPayRequestPaymentParams payParams,
        LocalDateTime expireTime,
        String failReason
    ) {
        return new AdminWalletRechargeResult(
            rechargeOrder != null ? rechargeOrder.getRechargeOrderNo() : null,
            payment != null ? payment.getPaymentNo() : null,
            walletId,
            rechargeOrder != null ? rechargeOrder.getUserId() : null,
            rechargeOrder != null ? rechargeOrder.getStoreId() : null,
            rechargeOrder != null ? rechargeOrder.getRechargeProductId() : null,
            rechargeOrder != null ? rechargeOrder.getPayAmount() : null,
            rechargeOrder != null ? rechargeOrder.getPrincipalAmount() : null,
            rechargeOrder != null ? rechargeOrder.getGiftAmount() : null,
            payStatus,
            payParams,
            expireTime,
            failReason
        );
    }

    private UserStoreWallet buildEmptyWallet(Long userId, Long storeId) {
        UserStoreWallet wallet = new UserStoreWallet();
        wallet.setUserId(userId);
        wallet.setStoreId(storeId);
        wallet.setPrincipalBalance(BigDecimal.ZERO);
        wallet.setAvailablePrincipalBalance(BigDecimal.ZERO);
        wallet.setFrozenPrincipalBalance(BigDecimal.ZERO);
        wallet.setGiftBalance(BigDecimal.ZERO);
        wallet.setAvailableGiftBalance(BigDecimal.ZERO);
        wallet.setFrozenGiftBalance(BigDecimal.ZERO);
        wallet.setTotalRechargePrincipal(BigDecimal.ZERO);
        wallet.setTotalRechargeGift(BigDecimal.ZERO);
        wallet.setTotalConsumePrincipal(BigDecimal.ZERO);
        wallet.setTotalConsumeGift(BigDecimal.ZERO);
        wallet.setTotalRefundPrincipal(BigDecimal.ZERO);
        wallet.setStatus(1);
        wallet.setVersion(0);
        return wallet;
    }

    private String buildRechargeRemark(String source, String amountType, String remark) {
        String safeSource = StringUtils.hasText(source) ? source.trim() : "recharge";
        String prefix = "principal".equals(amountType)
            ? safeSource + " principal recharge"
            : safeSource + " gift recharge";
        if (StringUtils.hasText(remark)) {
            return prefix + ": " + remark;
        }
        return prefix;
    }

    private BigDecimal normalizeAmount(BigDecimal amount) {
        return amount != null ? amount : BigDecimal.ZERO;
    }

    private BigDecimal resolveAmount(BigDecimal amount) {
        return amount != null ? amount : BigDecimal.ZERO;
    }

    private BigDecimal resolvePrincipalBalance(UserStoreWallet wallet) {
        if (wallet == null) {
            return BigDecimal.ZERO;
        }
        return resolveAmount(wallet.getPrincipalBalance());
    }

    private BigDecimal resolveGiftBalance(UserStoreWallet wallet) {
        if (wallet == null) {
            return BigDecimal.ZERO;
        }
        return resolveAmount(wallet.getGiftBalance());
    }

    private BigDecimal resolveAvailablePrincipal(UserStoreWallet wallet) {
        if (wallet == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal available = wallet.getAvailablePrincipalBalance();
        if (available != null) {
            return normalizeAmount(available);
        }
        return resolvePrincipalBalance(wallet);
    }

    private BigDecimal resolveAvailableGift(UserStoreWallet wallet) {
        if (wallet == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal available = wallet.getAvailableGiftBalance();
        if (available != null) {
            return normalizeAmount(available);
        }
        return resolveGiftBalance(wallet);
    }

    private static final class RechargeExecutionContext {
        private final AdminWalletRechargeRequest request;
        private final BigDecimal principalAmount;
        private final BigDecimal giftAmount;
        private final BigDecimal payAmount;
        private final UserStoreWallet wallet;

        private RechargeExecutionContext(
            AdminWalletRechargeRequest request,
            BigDecimal principalAmount,
            BigDecimal giftAmount,
            BigDecimal payAmount,
            UserStoreWallet wallet
        ) {
            this.request = request;
            this.principalAmount = principalAmount;
            this.giftAmount = giftAmount;
            this.payAmount = payAmount;
            this.wallet = wallet;
        }
    }

    private static final class WalletRechargeResult {
        private final Long walletId;

        private WalletRechargeResult(Long walletId) {
            this.walletId = walletId;
        }
    }
}
