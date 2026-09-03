package com.washer.backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.washer.backend.common.ApiResponse;
import com.washer.backend.dto.admin.AdminWalletRechargeRequest;
import com.washer.backend.dto.admin.AdminWalletRechargeResult;
import com.washer.backend.entity.Store;
import com.washer.backend.entity.MembershipOrder;
import com.washer.backend.entity.UserDailyDiscountRecord;
import com.washer.backend.entity.UserInfo;
import com.washer.backend.entity.UserStoreWallet;
import com.washer.backend.entity.WalletTransaction;
import com.washer.backend.entity.WalletRechargeProduct;
import com.washer.backend.entity.WashOrder;
import com.washer.backend.entity.WashOrderPaymentDetail;
import com.washer.backend.mapper.UserDailyDiscountRecordMapper;
import com.washer.backend.mapper.UserStoreWalletMapper;
import com.washer.backend.mapper.WalletTransactionMapper;
import com.washer.backend.mapper.WalletRechargeProductMapper;
import com.washer.backend.mapper.WashOrderMapper;
import com.washer.backend.mapper.WashOrderPaymentDetailMapper;
import com.washer.backend.service.AdminWalletRechargeService;
import com.washer.backend.service.StoreService;
import com.washer.backend.service.UserInfoService;
import com.washer.backend.service.MembershipService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
public class MiniWalletController {

    private static final Logger LOGGER = LoggerFactory.getLogger(MiniWalletController.class);

    private static final List<DefaultRechargeProductTemplate> DEFAULT_RECHARGE_PRODUCTS = List.of(
        new DefaultRechargeProductTemplate("充值58元", new BigDecimal("58.00"), new BigDecimal("58.00"), BigDecimal.ZERO, 0),
        new DefaultRechargeProductTemplate("充值98赠15", new BigDecimal("98.00"), new BigDecimal("98.00"), new BigDecimal("15.00"), 0),
        new DefaultRechargeProductTemplate("充值198赠40", new BigDecimal("198.00"), new BigDecimal("198.00"), new BigDecimal("40.00"), 0),
        new DefaultRechargeProductTemplate("充值298赠80", new BigDecimal("298.00"), new BigDecimal("298.00"), new BigDecimal("80.00"), 0),
        new DefaultRechargeProductTemplate("充值498赠150", new BigDecimal("498.00"), new BigDecimal("498.00"), new BigDecimal("150.00"), 0),
        new DefaultRechargeProductTemplate("充值1000赠300", new BigDecimal("1000.00"), new BigDecimal("1000.00"), new BigDecimal("300.00"), 0)
    );

    private final AdminWalletRechargeService adminWalletRechargeService;
    private final StoreService storeService;
    private final UserInfoService userInfoService;
    private final MembershipService membershipService;
    private final UserStoreWalletMapper userStoreWalletMapper;
    private final WalletTransactionMapper walletTransactionMapper;
    private final WalletRechargeProductMapper walletRechargeProductMapper;
    private final WashOrderMapper washOrderMapper;
    private final WashOrderPaymentDetailMapper washOrderPaymentDetailMapper;
    private final UserDailyDiscountRecordMapper userDailyDiscountRecordMapper;

    public MiniWalletController(
        AdminWalletRechargeService adminWalletRechargeService,
        StoreService storeService,
        UserInfoService userInfoService,
        MembershipService membershipService,
        UserStoreWalletMapper userStoreWalletMapper,
        WalletTransactionMapper walletTransactionMapper,
        WalletRechargeProductMapper walletRechargeProductMapper,
        WashOrderMapper washOrderMapper,
        WashOrderPaymentDetailMapper washOrderPaymentDetailMapper,
        UserDailyDiscountRecordMapper userDailyDiscountRecordMapper
    ) {
        this.adminWalletRechargeService = adminWalletRechargeService;
        this.storeService = storeService;
        this.userInfoService = userInfoService;
        this.membershipService = membershipService;
        this.userStoreWalletMapper = userStoreWalletMapper;
        this.walletTransactionMapper = walletTransactionMapper;
        this.walletRechargeProductMapper = walletRechargeProductMapper;
        this.washOrderMapper = washOrderMapper;
        this.washOrderPaymentDetailMapper = washOrderPaymentDetailMapper;
        this.userDailyDiscountRecordMapper = userDailyDiscountRecordMapper;
    }

    @GetMapping("/wallet/recharge-products")
    public ApiResponse<List<Map<String, Object>>> rechargeProducts(@RequestParam Long storeId) {
        Store store = requireStore(storeId);
        List<WalletRechargeProduct> products = findActiveRechargeProducts(store.getId());
        if (products.isEmpty() && countRechargeProducts(store.getId()) == 0) {
            products = createDefaultRechargeProducts(store.getId());
        }
        return ApiResponse.success(products.stream().map(this::toRechargeProductResult).toList());
    }

    @PostMapping("/pay/recharge")
    public ApiResponse<AdminWalletRechargeResult> recharge(
        @RequestHeader Map<String, String> headers,
        @RequestBody Map<String, Object> payload
    ) {
        Long userId = resolveRechargeUserId(headers, payload);
        if (userId == null) {
            throw new IllegalArgumentException("userId is required");
        }

        Long storeId = parseLong(payload.get("storeId"));
        if (storeId == null) {
            storeId = parseLong(payload.get("store_id"));
        }
        if (storeId == null) {
            throw new IllegalArgumentException("storeId is required");
        }

        Long rechargeProductId = parseLong(payload.get("rechargeProductId"));
        if (rechargeProductId == null) {
            rechargeProductId = parseLong(payload.get("planId"));
        }
        if (rechargeProductId == null) {
            throw new IllegalArgumentException("rechargeProductId is required");
        }

        LOGGER.info(
            "recharge_create_started userId={}, storeId={}, rechargeProductId={}",
            userId,
            storeId,
            rechargeProductId
        );
        AdminWalletRechargeRequest request = new AdminWalletRechargeRequest();
        request.setUserId(userId);
        request.setStoreId(storeId);
        request.setRechargeProductId(rechargeProductId);
        request.setRemark("miniapp recharge order");

        try {
            AdminWalletRechargeResult result = adminWalletRechargeService.createMiniappRechargeOrder(request);
            LOGGER.info(
                "recharge_create_completed userId={}, rechargeOrderNo={}, walletId={}, status={}",
                userId,
                result.getRechargeOrderNo(),
                result.getWalletId(),
                result.getPayStatus()
            );
            return ApiResponse.success(result);
        } catch (RuntimeException exception) {
            LOGGER.error(
                "recharge_create_failed userId={}, storeId={}, rechargeProductId={}, reason={}",
                userId,
                storeId,
                rechargeProductId,
                exception.getMessage(),
                exception
            );
            throw exception;
        }
    }

    @GetMapping("/wallet/recharges/{rechargeOrderNo}")
    public ApiResponse<AdminWalletRechargeResult> getRechargeOrder(
        @PathVariable String rechargeOrderNo
    ) {
        return ApiResponse.success(adminWalletRechargeService.getRechargeOrderResult(rechargeOrderNo));
    }

    @PostMapping("/wallet/recharges/{rechargeOrderNo}/sync")
    public ApiResponse<AdminWalletRechargeResult> syncRechargeOrder(
        @PathVariable String rechargeOrderNo
    ) {
        return ApiResponse.success(adminWalletRechargeService.syncRechargeOrder(rechargeOrderNo));
    }

    @PostMapping("/pay/wxpay/notify")
    public Map<String, String> wechatPayNotify(
        @org.springframework.web.bind.annotation.RequestHeader Map<String, String> headers,
        @RequestBody String body
    ) {
        return adminWalletRechargeService.handleWechatPayNotify(headers, body);
    }

    @GetMapping("/wallet/summary")
    public ApiResponse<Map<String, Object>> summary(@RequestParam Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId is required");
        }
        List<UserStoreWallet> wallets = userStoreWalletMapper.selectList(
            new LambdaQueryWrapper<UserStoreWallet>()
                .eq(UserStoreWallet::getUserId, userId)
                .orderByAsc(UserStoreWallet::getId)
        );

        BigDecimal totalPrincipal = BigDecimal.ZERO;
        BigDecimal totalGift = BigDecimal.ZERO;
        for (UserStoreWallet wallet : wallets) {
            totalPrincipal = totalPrincipal.add(resolveAvailablePrincipal(wallet));
            totalGift = totalGift.add(resolveAvailableGift(wallet));
        }
        UserInfo user = userInfoService.getById(userId);

        Map<String, Object> result = new HashMap<>();
        result.put("principalBalance", totalPrincipal);
        result.put("giftBalance", totalGift);
        result.put("totalBalance", totalPrincipal.add(totalGift));
        result.put("points", user != null && user.getPoints() != null ? user.getPoints() : 0);
        return ApiResponse.success(result);
    }

    @GetMapping("/wallet/store-balances")
    public ApiResponse<Map<String, Object>> storeBalances(@RequestParam Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId is required");
        }

        List<UserStoreWallet> wallets = userStoreWalletMapper.selectList(
            new LambdaQueryWrapper<UserStoreWallet>()
                .eq(UserStoreWallet::getUserId, userId)
                .orderByAsc(UserStoreWallet::getStoreId)
        );

        Set<Long> storeIds = wallets.stream()
            .map(UserStoreWallet::getStoreId)
            .filter(id -> id != null && id > 0)
            .collect(Collectors.toSet());

        Map<Long, Store> storeMap = storeIds.isEmpty()
            ? new HashMap<>()
            : storeService.listByIds(storeIds).stream()
                .collect(Collectors.toMap(Store::getId, store -> store));

        List<Map<String, Object>> records = wallets.stream()
            .map(wallet -> {
                Map<String, Object> item = new HashMap<>();
                Long storeId = wallet.getStoreId();
                Store store = storeMap.get(storeId);
                item.put("storeId", storeId);
                item.put("storeName", store != null ? store.getStoreName() : "Store " + storeId);
                item.put("principalBalance", resolvePrincipalDisplayBalance(wallet));
                item.put("giftBalance", resolveGiftDisplayBalance(wallet));
                return item;
            })
            .toList();

        Map<String, Object> result = new HashMap<>();
        result.put("records", records);
        return ApiResponse.success(result);
    }

    @GetMapping("/costomerflow/page")
    public ApiResponse<Map<String, Object>> page(
        @RequestParam(required = false) Long costomerId,
        @RequestParam(defaultValue = "10") long limit,
        @RequestParam(defaultValue = "1") long page,
        @RequestParam(required = false) String bizType
    ) {
        Long userId = costomerId;
        if (userId == null) {
            throw new IllegalArgumentException("costomerId is required");
        }

        if ("consume".equalsIgnoreCase(bizType)) {
            return consumePage(userId, limit, page);
        }

        LambdaQueryWrapper<WalletTransaction> wrapper = new LambdaQueryWrapper<WalletTransaction>()
            .eq(WalletTransaction::getUserId, userId)
            .eq(StringUtils.hasText(bizType), WalletTransaction::getBizType, bizType)
            .orderByDesc(WalletTransaction::getId);

        boolean includeMembershipOrders = "recharge".equalsIgnoreCase(bizType);
        long safeLimit = Math.max(1, limit);
        long safePage = Math.max(1, page);
        Page<WalletTransaction> pageData = walletTransactionMapper.selectPage(
            includeMembershipOrders ? new Page<>(1, safeLimit * safePage) : new Page<>(page, limit),
            wrapper
        );
        Map<Long, Store> storeMap = buildStoreMap(pageData.getRecords().stream()
            .map(WalletTransaction::getStoreId)
            .filter(id -> id != null && id > 0)
            .collect(Collectors.toSet()));

        List<Map<String, Object>> records = pageData.getRecords().stream()
            .map(tx -> {
                Map<String, Object> item = new HashMap<>();
                item.put("id", tx.getId());
                item.put("amount", tx.getAmount());
                item.put("bizType", tx.getBizType());
                item.put("relatedAction", tx.getRelatedAction());
                item.put("bizActionNo", tx.getBizActionNo());
                item.put("changeType", tx.getChangeType());
                item.put("amountType", tx.getAmountType());
                item.put("storeId", tx.getStoreId());
                Store store = storeMap.get(tx.getStoreId());
                item.put("storeName", store != null ? store.getStoreName() : "");
                item.put("createdAt", tx.getCreatedAt());
                item.put("relatedOrderId", tx.getRelatedOrderId());
                item.put("relatedOrderNo", tx.getRelatedOrderNo());
                item.put("remark", tx.getRemark());
                return item;
            })
            .collect(Collectors.toCollection(ArrayList::new));

        if (includeMembershipOrders) {
            records.addAll(membershipService.listUserOrders(userId).stream()
                .map(this::toMembershipLedgerItem)
                .toList());
            records.sort(Comparator.comparing(this::ledgerTime).reversed());
            int fromIndex = (int) Math.min(records.size(), (safePage - 1) * safeLimit);
            int toIndex = (int) Math.min(records.size(), fromIndex + safeLimit);
            records = new ArrayList<>(records.subList(fromIndex, toIndex));
        }

        Map<String, Object> result = new HashMap<>();
        result.put("records", records);
        result.put("total", includeMembershipOrders
            ? walletTransactionMapper.selectCount(wrapper) + membershipService.listUserOrders(userId).size()
            : pageData.getTotal());
        result.put("page", safePage);
        result.put("limit", safeLimit);
        return ApiResponse.success(result);
    }

    private Map<String, Object> toMembershipLedgerItem(MembershipOrder order) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", order.getId());
        item.put("amount", order.getPayAmount());
        item.put("bizType", "membership");
        item.put("relatedAction", "membership");
        item.put("bizActionNo", order.getOrderNo());
        item.put("changeType", "income");
        item.put("amountType", "membership");
        item.put("storeId", null);
        item.put("storeName", "");
        item.put("createdAt", order.getPayTime() != null ? order.getPayTime() : order.getCreatedAt());
        item.put("relatedOrderId", order.getId());
        item.put("relatedOrderNo", order.getOrderNo());
        item.put("remark", "会员充值");
        item.put("recordType", "membership");
        return item;
    }

    private String ledgerTime(Map<String, Object> item) {
        Object value = item.get("createdAt");
        return value == null ? "" : String.valueOf(value);
    }

    private ApiResponse<Map<String, Object>> consumePage(Long userId, long limit, long page) {
        long safeLimit = Math.max(1, limit);
        long safePage = Math.max(1, page);
        long fetchSize = safeLimit * safePage;

        Page<WashOrder> pageData = washOrderMapper.selectPage(
            new Page<>(1, fetchSize),
            new LambdaQueryWrapper<WashOrder>()
                .eq(WashOrder::getUserId, userId)
                .eq(WashOrder::getPaymentStatus, "paid")
                .orderByDesc(WashOrder::getSettleTime)
                .orderByDesc(WashOrder::getId)
        );
        List<WalletTransaction> fineTransactions = walletTransactionMapper.selectList(
            new LambdaQueryWrapper<WalletTransaction>()
                .eq(WalletTransaction::getUserId, userId)
                .eq(WalletTransaction::getBizType, "fine")
                .eq(WalletTransaction::getChangeType, "out")
                .orderByDesc(WalletTransaction::getCreatedAt)
                .orderByDesc(WalletTransaction::getId)
        );

        List<WashOrder> orders = pageData.getRecords();
        List<Long> orderIds = orders.stream()
            .map(WashOrder::getId)
            .toList();
        Set<Long> storeIds = orders.stream()
            .map(WashOrder::getStoreId)
            .filter(id -> id != null && id > 0)
            .collect(Collectors.toSet());
        fineTransactions.stream()
            .map(WalletTransaction::getStoreId)
            .filter(id -> id != null && id > 0)
            .forEach(storeIds::add);
        Map<Long, Store> storeMap = buildStoreMap(storeIds);
        Map<Long, List<WashOrderPaymentDetail>> paymentDetailMap = buildPaymentDetailMap(orderIds);
        Map<Long, UserDailyDiscountRecord> discountRecordMap = buildDiscountRecordMap(orderIds);

        List<Map<String, Object>> mergedRecords = new ArrayList<>();
        List<Map<String, Object>> fineRecords = toFineConsumeRecords(fineTransactions, storeMap);
        mergedRecords.addAll(orders.stream()
            .map(order -> toConsumeRecord(
                order,
                storeMap.get(order.getStoreId()),
                paymentDetailMap.getOrDefault(order.getId(), List.of()),
                discountRecordMap.get(order.getId())
            ))
            .toList());
        mergedRecords.addAll(fineRecords);
        mergedRecords.sort(this::compareConsumeRecordTimeDesc);

        int fromIndex = (int) Math.min((safePage - 1) * safeLimit, mergedRecords.size());
        int toIndex = (int) Math.min(fromIndex + safeLimit, mergedRecords.size());
        List<Map<String, Object>> records = new ArrayList<>(mergedRecords.subList(fromIndex, toIndex));

        Map<String, Object> result = new HashMap<>();
        result.put("records", records);
        result.put("total", pageData.getTotal() + fineRecords.size());
        result.put("page", safePage);
        result.put("limit", safeLimit);
        return ApiResponse.success(result);
    }

    private List<Map<String, Object>> toFineConsumeRecords(
        List<WalletTransaction> fineTransactions,
        Map<Long, Store> storeMap
    ) {
        if (fineTransactions == null || fineTransactions.isEmpty()) {
            return List.of();
        }

        Map<String, List<WalletTransaction>> grouped = new LinkedHashMap<>();
        for (WalletTransaction transaction : fineTransactions) {
            String key = StringUtils.hasText(transaction.getBizActionNo())
                ? transaction.getBizActionNo()
                : "TX_" + transaction.getId();
            grouped.computeIfAbsent(key, ignored -> new ArrayList<>()).add(transaction);
        }

        return grouped.values().stream()
            .map(transactions -> toFineConsumeRecord(transactions, storeMap))
            .toList();
    }

    private Map<String, Object> toFineConsumeRecord(
        List<WalletTransaction> transactions,
        Map<Long, Store> storeMap
    ) {
        WalletTransaction first = transactions.get(0);
        BigDecimal amount = transactions.stream()
            .map(WalletTransaction::getAmount)
            .map(this::normalizeAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        Store store = storeMap.get(first.getStoreId());

        Map<String, Object> item = new HashMap<>();
        item.put("id", first.getId());
        item.put("recordType", "fine");
        item.put("bizType", "fine");
        item.put("relatedAction", "fine");
        item.put("bizActionNo", first.getBizActionNo());
        item.put("orderNo", first.getBizActionNo());
        item.put("relatedOrderNo", first.getBizActionNo());
        item.put("storeId", first.getStoreId());
        item.put("storeName", store != null ? store.getStoreName() : "Store " + first.getStoreId());
        item.put("createdAt", first.getCreatedAt());
        item.put("payMode", "fine");
        item.put("payTypeText", "罚款扣款");
        item.put("amount", amount);
        item.put("paidAmount", amount);
        item.put("finalAmount", amount);
        item.put("discountAmount", BigDecimal.ZERO);
        item.put("hasDiscount", false);
        item.put("discountText", "");
        item.put("cardDeductTimes", 0);
        item.put("remark", first.getRemark());
        item.put("paymentParts", transactions.stream().map(this::toFinePaymentPart).toList());
        return item;
    }

    private Map<String, Object> toFinePaymentPart(WalletTransaction transaction) {
        Map<String, Object> item = new HashMap<>();
        item.put("sourceType", "fine");
        item.put("amountType", transaction.getAmountType());
        item.put("amount", normalizeAmount(transaction.getAmount()));
        item.put("deductTimes", 0);
        item.put("label", resolveFinePaymentPartLabel(transaction));
        return item;
    }

    private String resolveFinePaymentPartLabel(WalletTransaction transaction) {
        String amountText = normalizeAmount(transaction.getAmount()).toPlainString();
        if ("gift".equals(transaction.getAmountType())) {
            return "赠送余额扣罚 " + amountText + " 元";
        }
        return "通用余额扣罚 " + amountText + " 元";
    }

    private int compareConsumeRecordTimeDesc(Map<String, Object> left, Map<String, Object> right) {
        LocalDateTime leftTime = left.get("createdAt") instanceof LocalDateTime time ? time : LocalDateTime.MIN;
        LocalDateTime rightTime = right.get("createdAt") instanceof LocalDateTime time ? time : LocalDateTime.MIN;
        int timeCompare = rightTime.compareTo(leftTime);
        if (timeCompare != 0) {
            return timeCompare;
        }
        return Long.compare(toLong(right.get("id")), toLong(left.get("id")));
    }

    private Map<Long, List<WashOrderPaymentDetail>> buildPaymentDetailMap(List<Long> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            return Map.of();
        }
        return washOrderPaymentDetailMapper.selectList(
            new LambdaQueryWrapper<WashOrderPaymentDetail>()
                .in(WashOrderPaymentDetail::getOrderId, orderIds)
                .orderByAsc(WashOrderPaymentDetail::getPaymentSeq)
                .orderByAsc(WashOrderPaymentDetail::getId)
        ).stream()
            .collect(Collectors.groupingBy(WashOrderPaymentDetail::getOrderId));
    }

    private Map<Long, UserDailyDiscountRecord> buildDiscountRecordMap(List<Long> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            return Map.of();
        }
        return userDailyDiscountRecordMapper.selectList(
            new LambdaQueryWrapper<UserDailyDiscountRecord>()
                .in(UserDailyDiscountRecord::getOrderId, orderIds)
                .orderByDesc(UserDailyDiscountRecord::getId)
        ).stream()
            .collect(Collectors.toMap(
                UserDailyDiscountRecord::getOrderId,
                record -> record,
                (left, right) -> left
            ));
    }

    private Map<String, Object> toConsumeRecord(
        WashOrder order,
        Store store,
        List<WashOrderPaymentDetail> paymentDetails,
        UserDailyDiscountRecord discountRecord
    ) {
        BigDecimal discountAmount = resolveDiscountAmount(order, discountRecord);
        int cardDeductTimes = resolveCardDeductTimes(order, paymentDetails);
        boolean cardPaid = cardDeductTimes > 0 || "card".equals(order.getPayMode());
        boolean walletPaid = paymentDetails.stream().anyMatch(detail -> "wallet".equals(detail.getSourceType()))
            || "wallet".equals(order.getPayMode());
        boolean hasDiscount = discountAmount.compareTo(BigDecimal.ZERO) > 0
            || (order.getIsFirstPeriodDiscountUsed() != null && order.getIsFirstPeriodDiscountUsed() == 1)
            || discountRecord != null;

        Map<String, Object> item = new HashMap<>();
        item.put("id", order.getId());
        item.put("recordType", "consume");
        item.put("bizType", "consume");
        item.put("orderId", order.getId());
        item.put("orderNo", order.getOrderNo());
        item.put("storeId", order.getStoreId());
        item.put("storeName", store != null ? store.getStoreName() : "Store " + order.getStoreId());
        item.put("createdAt", resolveConsumeTime(order));
        item.put("payMode", order.getPayMode());
        item.put("payTypeText", resolvePayTypeText(walletPaid, cardPaid, hasDiscount));
        item.put("amount", resolveDisplayConsumeAmount(order, cardDeductTimes, cardPaid));
        item.put("paidAmount", normalizeAmount(order.getPaidAmount()));
        item.put("finalAmount", normalizeAmount(order.getFinalAmount()));
        item.put("discountAmount", discountAmount);
        item.put("hasDiscount", hasDiscount);
        item.put("discountText", resolveDiscountText(discountRecord, discountAmount));
        item.put("cardDeductTimes", cardDeductTimes);
        item.put("paymentParts", paymentDetails.stream().map(this::toPaymentPart).toList());
        return item;
    }

    private Map<String, Object> toPaymentPart(WashOrderPaymentDetail detail) {
        Map<String, Object> item = new HashMap<>();
        item.put("sourceType", detail.getSourceType());
        item.put("amountType", detail.getAmountType());
        item.put("amount", normalizeAmount(detail.getAmount()));
        item.put("deductTimes", detail.getDeductTimes() != null ? detail.getDeductTimes() : 0);
        item.put("label", resolvePaymentPartLabel(detail));
        return item;
    }

    private String resolvePaymentPartLabel(WashOrderPaymentDetail detail) {
        if ("card".equals(detail.getSourceType())) {
            int deductTimes = detail.getDeductTimes() != null ? detail.getDeductTimes() : 1;
            return "次卡支付 " + deductTimes + " 次";
        }
        if ("gift".equals(detail.getAmountType())) {
            return "赠送余额 " + normalizeAmount(detail.getAmount()).toPlainString() + " 元";
        }
        if ("principal".equals(detail.getAmountType())) {
            return "钱包余额 " + normalizeAmount(detail.getAmount()).toPlainString() + " 元";
        }
        return "钱包支付 " + normalizeAmount(detail.getAmount()).toPlainString() + " 元";
    }

    private String resolvePayTypeText(boolean walletPaid, boolean cardPaid, boolean hasDiscount) {
        String base;
        if (walletPaid && cardPaid) {
            base = "混合支付";
        } else if (cardPaid) {
            base = "次卡支付";
        } else {
            base = "钱包支付";
        }
        return hasDiscount ? base + " + 优惠券" : base;
    }

    private Object resolveDisplayConsumeAmount(WashOrder order, int cardDeductTimes, boolean cardPaid) {
        if (cardPaid && cardDeductTimes > 0) {
            return cardDeductTimes;
        }
        return normalizeAmount(order.getPaidAmount());
    }

    private int resolveCardDeductTimes(WashOrder order, List<WashOrderPaymentDetail> paymentDetails) {
        int detailTimes = paymentDetails.stream()
            .filter(detail -> "card".equals(detail.getSourceType()))
            .map(WashOrderPaymentDetail::getDeductTimes)
            .filter(times -> times != null && times > 0)
            .reduce(0, Integer::sum);
        if (detailTimes > 0) {
            return detailTimes;
        }
        return order.getCardDeductTimes() != null ? order.getCardDeductTimes() : 0;
    }

    private BigDecimal resolveDiscountAmount(WashOrder order, UserDailyDiscountRecord discountRecord) {
        BigDecimal orderDiscount = normalizeAmount(order.getFirstPeriodDiscountAmount());
        if (orderDiscount.compareTo(BigDecimal.ZERO) > 0) {
            return orderDiscount;
        }
        return discountRecord != null ? normalizeAmount(discountRecord.getDiscountAmount()) : BigDecimal.ZERO;
    }

    private String resolveDiscountText(UserDailyDiscountRecord discountRecord, BigDecimal discountAmount) {
        if (discountAmount.compareTo(BigDecimal.ZERO) <= 0 && discountRecord == null) {
            return "";
        }
        String name = discountRecord != null && "first_period_discount".equals(discountRecord.getDiscountType())
            ? "首段优惠券"
            : "优惠券";
        if (discountAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return name;
        }
        return name + " -" + discountAmount.toPlainString() + " 元";
    }

    private LocalDateTime resolveConsumeTime(WashOrder order) {
        if (order.getSettleTime() != null) {
            return order.getSettleTime();
        }
        if (order.getEndTime() != null) {
            return order.getEndTime();
        }
        if (order.getUpdatedAt() != null) {
            return order.getUpdatedAt();
        }
        return order.getCreatedAt();
    }

    private Map<Long, Store> buildStoreMap(Set<Long> storeIds) {
        if (storeIds == null || storeIds.isEmpty()) {
            return Map.of();
        }
        return storeService.listByIds(storeIds).stream()
            .collect(Collectors.toMap(Store::getId, store -> store, (left, right) -> left));
    }

    private Store requireStore(Long storeId) {
        if (storeId == null) {
            throw new IllegalArgumentException("storeId is required");
        }
        Store store = storeService.getById(storeId);
        if (store == null) {
            throw new IllegalArgumentException("store not found");
        }
        return store;
    }

    private List<WalletRechargeProduct> findActiveRechargeProducts(Long storeId) {
        if (storeId == null) {
            return List.of();
        }
        LocalDateTime now = LocalDateTime.now();
        return walletRechargeProductMapper.selectList(
            new LambdaQueryWrapper<WalletRechargeProduct>()
                .eq(WalletRechargeProduct::getStoreId, storeId)
                .eq(WalletRechargeProduct::getStatus, 1)
                .and(wrapper -> wrapper.isNull(WalletRechargeProduct::getEffectiveTime).or().le(WalletRechargeProduct::getEffectiveTime, now))
                .and(wrapper -> wrapper.isNull(WalletRechargeProduct::getExpireTime).or().gt(WalletRechargeProduct::getExpireTime, now))
                .orderByAsc(WalletRechargeProduct::getPayAmount)
                .orderByAsc(WalletRechargeProduct::getId)
        );
    }

    private long countRechargeProducts(Long storeId) {
        Long count = walletRechargeProductMapper.selectCount(
            new LambdaQueryWrapper<WalletRechargeProduct>()
                .eq(WalletRechargeProduct::getStoreId, storeId)
        );
        return count != null ? count : 0;
    }

    private List<WalletRechargeProduct> createDefaultRechargeProducts(Long storeId) {
        List<WalletRechargeProduct> products = new ArrayList<>(DEFAULT_RECHARGE_PRODUCTS.size());
        for (DefaultRechargeProductTemplate template : DEFAULT_RECHARGE_PRODUCTS) {
            WalletRechargeProduct product = new WalletRechargeProduct();
            product.setStoreId(storeId);
            product.setProductName(template.productName());
            product.setPayAmount(template.payAmount());
            product.setPrincipalAmount(template.principalAmount());
            product.setGiftAmount(template.giftAmount());
            product.setPurchaseLimit(template.purchaseLimit());
            product.setStatus(1);
            walletRechargeProductMapper.insert(product);
            products.add(product);
        }
        return products;
    }

    private Map<String, Object> toRechargeProductResult(WalletRechargeProduct product) {
        Map<String, Object> result = new HashMap<>();
        result.put("id", product.getId());
        result.put("rechargeProductId", product.getId());
        result.put("planId", product.getId());
        result.put("storeId", product.getStoreId());
        result.put("productName", product.getProductName());
        result.put("title", product.getProductName());
        result.put("payAmount", product.getPayAmount());
        result.put("principalAmount", product.getPrincipalAmount());
        result.put("giftAmount", product.getGiftAmount());
        result.put("price", product.getPayAmount());
        result.put("value", product.getPayAmount());
        result.put("gift", product.getGiftAmount());
        result.put("purchaseLimit", product.getPurchaseLimit());
        result.put("status", product.getStatus());
        return result;
    }

    private Long resolveRechargeUserId(Map<String, String> headers, Map<String, Object> payload) {
        String openid = normalizeText(getHeader(headers, "X-Washer-Openid", "x-washer-openid"));
        if (!StringUtils.hasText(openid)) {
            openid = normalizeText(getString(payload, "openid", "openId"));
        }
        if (StringUtils.hasText(openid)) {
            UserInfo user = userInfoService.lambdaQuery()
                .eq(UserInfo::getOpenid, openid)
                .last("limit 1")
                .one();
            if (user == null || user.getId() == null) {
                throw new IllegalArgumentException("current user not found");
            }
            return user.getId();
        }
        return parseLong(payload.get("userId"));
    }

    private String getHeader(Map<String, String> headers, String... keys) {
        if (headers == null || keys == null) {
            return null;
        }
        for (String key : keys) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(key)) {
                    return entry.getValue();
                }
            }
        }
        return null;
    }

    private String getString(Map<String, Object> payload, String... keys) {
        if (payload == null || keys == null) {
            return null;
        }
        for (String key : keys) {
            Object value = payload.get(key);
            if (value == null) {
                continue;
            }
            String text = String.valueOf(value);
            if (StringUtils.hasText(text)) {
                return text;
            }
        }
        return null;
    }

    private String normalizeText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private Long parseLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            String text = value.toString();
            if (!StringUtils.hasText(text)) {
                return null;
            }
            return Long.parseLong(text);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private long toLong(Object value) {
        Long parsed = parseLong(value);
        return parsed != null ? parsed : 0L;
    }

    private BigDecimal normalizeAmount(BigDecimal amount) {
        return amount != null ? amount : BigDecimal.ZERO;
    }

    private BigDecimal parseAmount(Map<String, Object> payload, String... keys) {
        if (payload == null || keys == null) {
            return null;
        }
        for (String key : keys) {
            Object value = payload.get(key);
            if (value == null) {
                continue;
            }
            if (value instanceof BigDecimal bigDecimal) {
                return bigDecimal;
            }
            if (value instanceof Number number) {
                return BigDecimal.valueOf(number.doubleValue());
            }
            try {
                String text = value.toString();
                if (StringUtils.hasText(text)) {
                    return new BigDecimal(text.trim());
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }

    private BigDecimal resolveAvailablePrincipal(UserStoreWallet wallet) {
        if (wallet == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal available = wallet.getAvailablePrincipalBalance();
        if (available != null) {
            return available;
        }
        return wallet.getPrincipalBalance() != null ? wallet.getPrincipalBalance() : BigDecimal.ZERO;
    }

    private BigDecimal resolveAvailableGift(UserStoreWallet wallet) {
        if (wallet == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal available = wallet.getAvailableGiftBalance();
        if (available != null) {
            return available;
        }
        return wallet.getGiftBalance() != null ? wallet.getGiftBalance() : BigDecimal.ZERO;
    }

    private BigDecimal resolveGiftDisplayBalance(UserStoreWallet wallet) {
        if (wallet == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal available = wallet.getAvailableGiftBalance();
        if (available != null && available.compareTo(BigDecimal.ZERO) > 0) {
            return available;
        }
        BigDecimal giftBalance = wallet.getGiftBalance();
        if (giftBalance != null && giftBalance.compareTo(BigDecimal.ZERO) > 0) {
            return giftBalance;
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal resolvePrincipalDisplayBalance(UserStoreWallet wallet) {
        if (wallet == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal available = wallet.getAvailablePrincipalBalance();
        if (available != null && available.compareTo(BigDecimal.ZERO) > 0) {
            return available;
        }
        BigDecimal principalBalance = wallet.getPrincipalBalance();
        if (principalBalance != null && principalBalance.compareTo(BigDecimal.ZERO) > 0) {
            return principalBalance;
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal resolveGiftAmount(BigDecimal principalAmount) {
        if (principalAmount == null) {
            return BigDecimal.ZERO;
        }
        int amount = principalAmount.intValue();
        return switch (amount) {
            case 58 -> new BigDecimal("0");
            case 98 -> new BigDecimal("15");
            case 198 -> new BigDecimal("40");
            case 298 -> new BigDecimal("80");
            case 498 -> new BigDecimal("150");
            case 1000 -> new BigDecimal("300");
            default -> BigDecimal.ZERO;
        };
    }

    private record DefaultRechargeProductTemplate(
        String productName,
        BigDecimal payAmount,
        BigDecimal principalAmount,
        BigDecimal giftAmount,
        Integer purchaseLimit
    ) {
    }
}
