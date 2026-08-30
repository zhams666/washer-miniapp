package com.washer.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.washer.backend.dto.admin.AdminDashboardActivityItem;
import com.washer.backend.dto.admin.AdminDashboardChannelMetric;
import com.washer.backend.dto.admin.AdminDashboardDailyTrendPoint;
import com.washer.backend.dto.admin.AdminDashboardDeviceAlertItem;
import com.washer.backend.dto.admin.AdminDashboardDeviceStatus;
import com.washer.backend.dto.admin.AdminDashboardHourlyPoint;
import com.washer.backend.dto.admin.AdminDashboardKpiItem;
import com.washer.backend.dto.admin.AdminDashboardMetric;
import com.washer.backend.dto.admin.AdminDashboardStoreMetric;
import com.washer.backend.dto.admin.AdminDashboardTodayOverview;
import com.washer.backend.entity.CardPurchaseOrder;
import com.washer.backend.entity.CardUsageRecord;
import com.washer.backend.entity.Device;
import com.washer.backend.entity.RechargeOrder;
import com.washer.backend.entity.Store;
import com.washer.backend.entity.UserCard;
import com.washer.backend.entity.UserInfo;
import com.washer.backend.entity.UserStoreWallet;
import com.washer.backend.entity.WalletTransaction;
import com.washer.backend.entity.WashOrder;
import com.washer.backend.mapper.CardPurchaseOrderMapper;
import com.washer.backend.mapper.CardUsageRecordMapper;
import com.washer.backend.mapper.DeviceMapper;
import com.washer.backend.mapper.RechargeOrderMapper;
import com.washer.backend.mapper.UserCardMapper;
import com.washer.backend.mapper.UserInfoMapper;
import com.washer.backend.mapper.UserStoreWalletMapper;
import com.washer.backend.mapper.WalletTransactionMapper;
import com.washer.backend.mapper.WashOrderMapper;
import com.washer.backend.service.AdminDashboardService;
import com.washer.backend.service.StoreService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private static final String PAY_STATUS_PAID = "paid";
    private static final String WALLET_BIZ_CONSUME = "consume";
    private static final String WALLET_CHANGE_OUT = "out";
    private static final Set<String> GROUP_BUY_CHANNELS = Set.of("douyin", "meituan", "dazhong");

    private final WalletTransactionMapper walletTransactionMapper;
    private final RechargeOrderMapper rechargeOrderMapper;
    private final CardPurchaseOrderMapper cardPurchaseOrderMapper;
    private final CardUsageRecordMapper cardUsageRecordMapper;
    private final WashOrderMapper washOrderMapper;
    private final DeviceMapper deviceMapper;
    private final UserInfoMapper userInfoMapper;
    private final UserStoreWalletMapper userStoreWalletMapper;
    private final UserCardMapper userCardMapper;
    private final StoreService storeService;

    public AdminDashboardServiceImpl(
        WalletTransactionMapper walletTransactionMapper,
        RechargeOrderMapper rechargeOrderMapper,
        CardPurchaseOrderMapper cardPurchaseOrderMapper,
        CardUsageRecordMapper cardUsageRecordMapper,
        WashOrderMapper washOrderMapper,
        DeviceMapper deviceMapper,
        UserInfoMapper userInfoMapper,
        UserStoreWalletMapper userStoreWalletMapper,
        UserCardMapper userCardMapper,
        StoreService storeService
    ) {
        this.walletTransactionMapper = walletTransactionMapper;
        this.rechargeOrderMapper = rechargeOrderMapper;
        this.cardPurchaseOrderMapper = cardPurchaseOrderMapper;
        this.cardUsageRecordMapper = cardUsageRecordMapper;
        this.washOrderMapper = washOrderMapper;
        this.deviceMapper = deviceMapper;
        this.userInfoMapper = userInfoMapper;
        this.userStoreWalletMapper = userStoreWalletMapper;
        this.userCardMapper = userCardMapper;
        this.storeService = storeService;
    }

    @Override
    public AdminDashboardTodayOverview getTodayOverview(LocalDate bizDate) {
        return getTodayOverview(bizDate, null);
    }

    @Override
    public AdminDashboardTodayOverview getTodayOverview(LocalDate bizDate, Long storeId) {
        LocalDate resolvedDate = bizDate != null ? bizDate : LocalDate.now();
        return getStatistics(resolvedDate, resolvedDate, storeId);
    }

    @Override
    public AdminDashboardTodayOverview getStatistics(LocalDate startDate, LocalDate endDate) {
        return getStatistics(startDate, endDate, null);
    }

    @Override
    public AdminDashboardTodayOverview getStatistics(LocalDate startDate, LocalDate endDate, Long storeId) {
        Long resolvedStoreId = normalizeStoreId(storeId);
        LocalDate today = LocalDate.now();
        LocalDate resolvedEndDate = endDate != null ? endDate : (startDate != null ? startDate : today);
        LocalDate resolvedStartDate = startDate != null ? startDate : resolvedEndDate;
        if (resolvedEndDate.isBefore(resolvedStartDate)) {
            LocalDate temp = resolvedStartDate;
            resolvedStartDate = resolvedEndDate;
            resolvedEndDate = temp;
        }

        LocalDateTime start = resolvedStartDate.atStartOfDay();
        LocalDateTime end = resolvedEndDate.plusDays(1).atStartOfDay();

        List<WashOrder> washOrders = loadWashOrders(start, end, resolvedStoreId);
        List<CardUsageRecord> cardUsages = loadCardUsages(start, end, resolvedStoreId);
        List<WalletTransaction> walletConsumes = loadWalletConsumes(start, end, resolvedStoreId);
        List<RechargeOrder> rechargeOrders = loadRechargeOrders(start, end, resolvedStoreId);
        List<CardPurchaseOrder> cardPurchases = loadCardPurchases(start, end, resolvedStoreId);
        List<UserCard> legacyManualCards = loadLegacyManualCards(start, end, resolvedStoreId);
        List<UserCard> groupBuyVoucherCards = loadGroupBuyVoucherCards(start, end, resolvedStoreId);

        Map<Long, Store> storeMap = buildStoreMap(cardUsages, walletConsumes, rechargeOrders, cardPurchases, legacyManualCards, groupBuyVoucherCards);

        AdminDashboardTodayOverview overview = new AdminDashboardTodayOverview();
        overview.setBizDate(resolvedStartDate);
        overview.setStartDate(resolvedStartDate);
        overview.setEndDate(resolvedEndDate);
        overview.setGeneratedAt(LocalDateTime.now());
        overview.setTopMetrics(buildTopMetrics(washOrders, groupBuyVoucherCards, walletConsumes, rechargeOrders, start, end, resolvedStoreId));
        overview.setRangeMetrics(buildRangeMetrics(washOrders, cardUsages, groupBuyVoucherCards, walletConsumes, rechargeOrders, cardPurchases, legacyManualCards, resolvedStoreId));
        overview.setDailyTrend(buildDailyTrend(resolvedStartDate, resolvedEndDate, washOrders, cardUsages, groupBuyVoucherCards, walletConsumes, rechargeOrders));
        overview.setSummaryCards(buildSummaryCards(cardUsages, walletConsumes, rechargeOrders, cardPurchases, legacyManualCards));
        overview.setHourlyTrend(buildHourlyTrend(cardUsages, walletConsumes, rechargeOrders, cardPurchases));
        overview.setStoreMetrics(buildStoreMetrics(cardUsages, walletConsumes, rechargeOrders, cardPurchases, legacyManualCards, storeMap));
        overview.setCardPurchaseChannels(buildCardPurchaseChannels(cardPurchases, legacyManualCards, groupBuyVoucherCards));
        overview.setDeviceStatus(buildDeviceStatus(resolvedStoreId));
        overview.setRecentActivities(buildRecentActivities(cardUsages, walletConsumes, rechargeOrders, cardPurchases, legacyManualCards, groupBuyVoucherCards, storeMap, 10));
        return overview;
    }

    @Override
    public List<AdminDashboardActivityItem> listRecentActivities(LocalDate startDate, LocalDate endDate, Integer limit) {
        return listRecentActivities(startDate, endDate, limit, null);
    }

    @Override
    public List<AdminDashboardActivityItem> listRecentActivities(LocalDate startDate, LocalDate endDate, Integer limit, Long storeId) {
        Long resolvedStoreId = normalizeStoreId(storeId);
        LocalDate today = LocalDate.now();
        LocalDate resolvedEndDate = endDate != null ? endDate : (startDate != null ? startDate : today);
        LocalDate resolvedStartDate = startDate != null ? startDate : resolvedEndDate.minusDays(6);
        if (resolvedEndDate.isBefore(resolvedStartDate)) {
            LocalDate temp = resolvedStartDate;
            resolvedStartDate = resolvedEndDate;
            resolvedEndDate = temp;
        }

        LocalDateTime start = resolvedStartDate.atStartOfDay();
        LocalDateTime end = resolvedEndDate.plusDays(1).atStartOfDay();

        List<CardUsageRecord> cardUsages = loadCardUsages(start, end, resolvedStoreId);
        List<WalletTransaction> walletConsumes = loadWalletConsumes(start, end, resolvedStoreId);
        List<RechargeOrder> rechargeOrders = loadRechargeOrders(start, end, resolvedStoreId);
        List<CardPurchaseOrder> cardPurchases = loadCardPurchases(start, end, resolvedStoreId);
        List<UserCard> legacyManualCards = loadLegacyManualCards(start, end, resolvedStoreId);
        List<UserCard> groupBuyVoucherCards = loadGroupBuyVoucherCards(start, end, resolvedStoreId);
        Map<Long, Store> storeMap = buildStoreMap(cardUsages, walletConsumes, rechargeOrders, cardPurchases, legacyManualCards, groupBuyVoucherCards);
        return buildRecentActivities(
            cardUsages,
            walletConsumes,
            rechargeOrders,
            cardPurchases,
            legacyManualCards,
            groupBuyVoucherCards,
            storeMap,
            normalizeActivityLimit(limit)
        );
    }

    private List<WashOrder> loadWashOrders(LocalDateTime start, LocalDateTime end, Long storeId) {
        return washOrderMapper.selectList(
            new LambdaQueryWrapper<WashOrder>()
                .eq(storeId != null, WashOrder::getStoreId, storeId)
                .and(wrapper -> wrapper
                    .ge(WashOrder::getStartTime, start)
                    .lt(WashOrder::getStartTime, end)
                    .or(inner -> inner
                        .isNull(WashOrder::getStartTime)
                        .ge(WashOrder::getCreatedAt, start)
                        .lt(WashOrder::getCreatedAt, end)
                    )
                )
                .orderByDesc(WashOrder::getCreatedAt)
        );
    }

    private List<CardUsageRecord> loadCardUsages(LocalDateTime start, LocalDateTime end, Long storeId) {
        return cardUsageRecordMapper.selectList(
            new LambdaQueryWrapper<CardUsageRecord>()
                .eq(storeId != null, CardUsageRecord::getStoreId, storeId)
                .ge(CardUsageRecord::getUsageTime, start)
                .lt(CardUsageRecord::getUsageTime, end)
                .orderByDesc(CardUsageRecord::getUsageTime)
        );
    }

    private List<WalletTransaction> loadWalletConsumes(LocalDateTime start, LocalDateTime end, Long storeId) {
        return walletTransactionMapper.selectList(
            new LambdaQueryWrapper<WalletTransaction>()
                .eq(storeId != null, WalletTransaction::getStoreId, storeId)
                .eq(WalletTransaction::getBizType, WALLET_BIZ_CONSUME)
                .eq(WalletTransaction::getChangeType, WALLET_CHANGE_OUT)
                .ge(WalletTransaction::getCreatedAt, start)
                .lt(WalletTransaction::getCreatedAt, end)
                .orderByDesc(WalletTransaction::getCreatedAt)
        );
    }

    private List<RechargeOrder> loadRechargeOrders(LocalDateTime start, LocalDateTime end, Long storeId) {
        return rechargeOrderMapper.selectList(
            new LambdaQueryWrapper<RechargeOrder>()
                .eq(storeId != null, RechargeOrder::getStoreId, storeId)
                .eq(RechargeOrder::getPayStatus, PAY_STATUS_PAID)
                .and(wrapper -> wrapper
                    .ge(RechargeOrder::getPayTime, start)
                    .lt(RechargeOrder::getPayTime, end)
                    .or(inner -> inner
                        .isNull(RechargeOrder::getPayTime)
                        .ge(RechargeOrder::getCreatedAt, start)
                        .lt(RechargeOrder::getCreatedAt, end)
                    )
                )
                .orderByDesc(RechargeOrder::getCreatedAt)
        );
    }

    private List<CardPurchaseOrder> loadCardPurchases(LocalDateTime start, LocalDateTime end, Long storeId) {
        return cardPurchaseOrderMapper.selectList(
            new LambdaQueryWrapper<CardPurchaseOrder>()
                .eq(storeId != null, CardPurchaseOrder::getStoreId, storeId)
                .eq(CardPurchaseOrder::getPayStatus, PAY_STATUS_PAID)
                .and(wrapper -> wrapper
                    .ge(CardPurchaseOrder::getPurchaseTime, start)
                    .lt(CardPurchaseOrder::getPurchaseTime, end)
                    .or(inner -> inner
                        .isNull(CardPurchaseOrder::getPurchaseTime)
                        .ge(CardPurchaseOrder::getCreatedAt, start)
                        .lt(CardPurchaseOrder::getCreatedAt, end)
                    )
                )
                .orderByDesc(CardPurchaseOrder::getCreatedAt)
        );
    }

    private List<UserCard> loadLegacyManualCards(LocalDateTime start, LocalDateTime end, Long storeId) {
        return userCardMapper.selectList(
            new LambdaQueryWrapper<UserCard>()
                .eq(storeId != null, UserCard::getStoreId, storeId)
                .eq(UserCard::getSourceChannel, "admin")
                .eq(UserCard::getCardProductId, 0L)
                .and(wrapper -> wrapper.isNull(UserCard::getExternalOrderNo).or().eq(UserCard::getExternalOrderNo, ""))
                .ge(UserCard::getCreatedAt, start)
                .lt(UserCard::getCreatedAt, end)
                .orderByDesc(UserCard::getCreatedAt)
        );
    }

    private List<UserCard> loadGroupBuyVoucherCards(LocalDateTime start, LocalDateTime end, Long storeId) {
        return userCardMapper.selectList(
            new LambdaQueryWrapper<UserCard>()
                .eq(storeId != null, UserCard::getStoreId, storeId)
                .in(UserCard::getSourceChannel, GROUP_BUY_CHANNELS)
                .isNotNull(UserCard::getExternalOrderNo)
                .ne(UserCard::getExternalOrderNo, "")
                .ge(UserCard::getCreatedAt, start)
                .lt(UserCard::getCreatedAt, end)
                .orderByDesc(UserCard::getCreatedAt)
        );
    }

    private List<AdminDashboardKpiItem> buildTopMetrics(
        List<WashOrder> washOrders,
        List<UserCard> groupBuyVoucherCards,
        List<WalletTransaction> walletConsumes,
        List<RechargeOrder> rechargeOrders,
        LocalDateTime start,
        LocalDateTime end,
        Long storeId
    ) {
        long washCount = countWashOrders(washOrders);
        BigDecimal consumeAmount = sumWalletTransactionAmount(walletConsumes);
        BigDecimal rechargeAmount = sumRechargePayAmount(rechargeOrders);
        long groupBuyVerifyCount = countDistinctVoucherCodes(groupBuyVoucherCards);
        long newUserCount = countNewUsers(start, end, storeId);

        return List.of(
            new AdminDashboardKpiItem("todayConsume", "今日消费", consumeAmount, countDistinctWalletActions(walletConsumes), "元", "钱包消费流水"),
            new AdminDashboardKpiItem("todayRecharge", "今日充值", rechargeAmount, (long) rechargeOrders.size(), "元", "已支付充值订单"),
            new AdminDashboardKpiItem("groupVerify", "团购核销(商)", BigDecimal.ZERO, groupBuyVerifyCount, "次", "抖音/美团/大众点评核销"),
            new AdminDashboardKpiItem("newUsers", "新增用户", BigDecimal.ZERO, newUserCount, "人", "区间新增注册用户")
        );
    }

    private List<AdminDashboardKpiItem> buildRangeMetrics(
        List<WashOrder> washOrders,
        List<CardUsageRecord> cardUsages,
        List<UserCard> groupBuyVoucherCards,
        List<WalletTransaction> walletConsumes,
        List<RechargeOrder> rechargeOrders,
        List<CardPurchaseOrder> cardPurchases,
        List<UserCard> legacyManualCards,
        Long storeId
    ) {
        long washCount = countWashOrders(washOrders);
        long cardUsageTimes = cardUsages.stream().mapToLong(item -> normalizeTimes(item.getUsedTimes())).sum();
        BigDecimal rechargeAmount = sumRechargePayAmount(rechargeOrders);
        BigDecimal consumeAmount = sumWalletTransactionAmount(walletConsumes);
        long walletOrderCount = countWalletWashOrders(washOrders);
        long monthlyCardOrderCount = cardPurchases.stream().filter(this::isMonthlyCardPurchase).count();
        long cardOrderCount = cardPurchases.stream().filter(order -> !isMonthlyCardPurchase(order)).count() + legacyManualCards.size();
        WalletBalanceSnapshot walletBalanceSnapshot = buildWalletBalanceSnapshot(storeId);
        long userCount = countUsers(storeId);

        return List.of(
            new AdminDashboardKpiItem("cardUsageTimes", "次卡使用", BigDecimal.ZERO, cardUsageTimes, "次", "区间全部次卡核销次数"),
            new AdminDashboardKpiItem("washCount", "洗车数量", BigDecimal.ZERO, washCount, "辆", "区间洗车订单数"),
            new AdminDashboardKpiItem("groupVerifyRange", "团购核销(商)", BigDecimal.ZERO, countDistinctVoucherCodes(groupBuyVoucherCards), "次", "区间抖音/美团/大众券号核销"),
            new AdminDashboardKpiItem("rechargeAmount", "充值金额", rechargeAmount, (long) rechargeOrders.size(), "元", "区间充值实付金额"),
            new AdminDashboardKpiItem("consumeAmount", "消费金额", consumeAmount, countDistinctWalletActions(walletConsumes), "元", "区间钱包消费金额"),
            new AdminDashboardKpiItem("walletOrderCount", "余额订单数", BigDecimal.ZERO, walletOrderCount, "单", "钱包支付洗车订单"),
            new AdminDashboardKpiItem("monthlyCardOrderCount", "月卡订单数", BigDecimal.ZERO, monthlyCardOrderCount, "单", "月卡商品订单"),
            new AdminDashboardKpiItem("cardOrderCount", "次卡订单数", BigDecimal.ZERO, cardOrderCount, "单", "次卡商品订单"),
            new AdminDashboardKpiItem("merchantTotalBalance", "商家总余额", walletBalanceSnapshot.totalBalance(), 0L, "元", "用户钱包本金+赠送余额"),
            new AdminDashboardKpiItem("merchantPrincipalBalance", "本金余额", walletBalanceSnapshot.principalBalance(), 0L, "元", "用户钱包本金余额"),
            new AdminDashboardKpiItem("merchantGiftBalance", "赠送余额", walletBalanceSnapshot.giftBalance(), 0L, "元", "用户钱包赠送余额"),
            new AdminDashboardKpiItem("merchantUsers", "用户总数", BigDecimal.ZERO, userCount, "人", "平台注册用户总数")
        );
    }

    private List<AdminDashboardDailyTrendPoint> buildDailyTrend(
        LocalDate startDate,
        LocalDate endDate,
        List<WashOrder> washOrders,
        List<CardUsageRecord> cardUsages,
        List<UserCard> groupBuyVoucherCards,
        List<WalletTransaction> walletConsumes,
        List<RechargeOrder> rechargeOrders
    ) {
        Map<LocalDate, AdminDashboardDailyTrendPoint> pointMap = new LinkedHashMap<>();
        LocalDate cursor = startDate;
        while (!cursor.isAfter(endDate)) {
            pointMap.put(cursor, new AdminDashboardDailyTrendPoint(
                cursor,
                String.format("%02d-%02d", cursor.getMonthValue(), cursor.getDayOfMonth()),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                0L,
                0L,
                0L
            ));
            cursor = cursor.plusDays(1);
        }

        for (WashOrder order : washOrders) {
            if (!isWashOrderCounted(order)) {
                continue;
            }
            AdminDashboardDailyTrendPoint point = pointMap.get(resolveOrderDate(order));
            if (point != null) {
                point.setWashCount(point.getWashCount() + 1);
            }
        }

        for (CardUsageRecord usage : cardUsages) {
            AdminDashboardDailyTrendPoint point = pointMap.get(resolveDate(usage.getUsageTime()));
            if (point != null) {
                point.setCardUsageTimes(point.getCardUsageTimes() + normalizeTimes(usage.getUsedTimes()));
            }
        }

        for (UserCard voucherCard : buildDistinctVoucherCards(groupBuyVoucherCards)) {
            AdminDashboardDailyTrendPoint point = pointMap.get(resolveDate(voucherCard.getCreatedAt()));
            if (point != null) {
                point.setGroupVerifyCount(point.getGroupVerifyCount() + 1);
            }
        }

        for (WalletTransaction tx : walletConsumes) {
            AdminDashboardDailyTrendPoint point = pointMap.get(resolveDate(tx.getCreatedAt()));
            if (point != null) {
                point.setConsumeAmount(point.getConsumeAmount().add(normalizeAmount(tx.getAmount())));
            }
        }

        for (RechargeOrder order : rechargeOrders) {
            AdminDashboardDailyTrendPoint point = pointMap.get(resolveDate(resolveRechargeBizTime(order)));
            if (point != null) {
                point.setRechargeAmount(point.getRechargeAmount().add(normalizeAmount(order.getPayAmount())));
            }
        }

        return new ArrayList<>(pointMap.values());
    }

    private List<AdminDashboardMetric> buildSummaryCards(
        List<CardUsageRecord> cardUsages,
        List<WalletTransaction> walletConsumes,
        List<RechargeOrder> rechargeOrders,
        List<CardPurchaseOrder> cardPurchases,
        List<UserCard> legacyManualCards
    ) {
        long cardUsageTimes = cardUsages.stream().mapToLong(item -> normalizeTimes(item.getUsedTimes())).sum();
        BigDecimal walletConsumeAmount = walletConsumes.stream()
            .map(tx -> normalizeAmount(tx.getAmount()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        long walletConsumeCount = countDistinctWalletActions(walletConsumes);
        BigDecimal rechargePayAmount = rechargeOrders.stream()
            .map(order -> normalizeAmount(order.getPayAmount()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal rechargeGiftAmount = rechargeOrders.stream()
            .map(order -> normalizeAmount(order.getGiftAmount()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal cardPurchaseAmount = cardPurchases.stream()
            .map(order -> normalizeAmount(order.getPayAmount()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        long purchasedCards = cardPurchases.stream().mapToLong(order -> normalizeTimes(order.getBuyCount())).sum() + legacyManualCards.size();

        List<AdminDashboardMetric> metrics = new ArrayList<>();
        metrics.add(new AdminDashboardMetric("cardUsage", "今日次卡使用", BigDecimal.ZERO, (long) cardUsages.size(), cardUsageTimes, BigDecimal.ZERO));
        metrics.add(new AdminDashboardMetric("walletConsume", "今日钱包消费", walletConsumeAmount, walletConsumeCount, 0L, BigDecimal.ZERO));
        metrics.add(new AdminDashboardMetric("walletRecharge", "今日钱包充值", rechargePayAmount, (long) rechargeOrders.size(), 0L, rechargeGiftAmount));
        metrics.add(new AdminDashboardMetric("cardPurchase", "今日次卡购买", cardPurchaseAmount, (long) cardPurchases.size() + legacyManualCards.size(), purchasedCards, BigDecimal.ZERO));
        return metrics;
    }

    private List<AdminDashboardHourlyPoint> buildHourlyTrend(
        List<CardUsageRecord> cardUsages,
        List<WalletTransaction> walletConsumes,
        List<RechargeOrder> rechargeOrders,
        List<CardPurchaseOrder> cardPurchases
    ) {
        Map<Integer, AdminDashboardHourlyPoint> pointMap = new LinkedHashMap<>();
        for (int hour = 0; hour < 24; hour += 1) {
            pointMap.put(hour, new AdminDashboardHourlyPoint(hour, String.format("%02d:00", hour), 0L, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
        }

        for (CardUsageRecord usage : cardUsages) {
            AdminDashboardHourlyPoint point = pointMap.get(resolveHour(usage.getUsageTime()));
            point.setCardUsageTimes(point.getCardUsageTimes() + normalizeTimes(usage.getUsedTimes()));
        }

        for (WalletTransaction tx : walletConsumes) {
            AdminDashboardHourlyPoint point = pointMap.get(resolveHour(tx.getCreatedAt()));
            point.setWalletConsumeAmount(point.getWalletConsumeAmount().add(normalizeAmount(tx.getAmount())));
        }

        for (RechargeOrder order : rechargeOrders) {
            AdminDashboardHourlyPoint point = pointMap.get(resolveHour(resolveRechargeBizTime(order)));
            point.setWalletRechargeAmount(point.getWalletRechargeAmount().add(normalizeAmount(order.getPayAmount())));
        }

        for (CardPurchaseOrder order : cardPurchases) {
            AdminDashboardHourlyPoint point = pointMap.get(resolveHour(resolveCardPurchaseBizTime(order)));
            point.setCardPurchaseAmount(point.getCardPurchaseAmount().add(normalizeAmount(order.getPayAmount())));
        }

        return new ArrayList<>(pointMap.values());
    }

    private List<AdminDashboardStoreMetric> buildStoreMetrics(
        List<CardUsageRecord> cardUsages,
        List<WalletTransaction> walletConsumes,
        List<RechargeOrder> rechargeOrders,
        List<CardPurchaseOrder> cardPurchases,
        List<UserCard> legacyManualCards,
        Map<Long, Store> storeMap
    ) {
        Map<Long, AdminDashboardStoreMetric> metricMap = new HashMap<>();

        for (CardUsageRecord usage : cardUsages) {
            AdminDashboardStoreMetric metric = resolveStoreMetric(metricMap, storeMap, usage.getStoreId());
            metric.setCardUsageTimes(metric.getCardUsageTimes() + normalizeTimes(usage.getUsedTimes()));
        }

        for (WalletTransaction tx : walletConsumes) {
            AdminDashboardStoreMetric metric = resolveStoreMetric(metricMap, storeMap, tx.getStoreId());
            metric.setWalletConsumeAmount(metric.getWalletConsumeAmount().add(normalizeAmount(tx.getAmount())));
        }

        for (RechargeOrder order : rechargeOrders) {
            AdminDashboardStoreMetric metric = resolveStoreMetric(metricMap, storeMap, order.getStoreId());
            metric.setWalletRechargeAmount(metric.getWalletRechargeAmount().add(normalizeAmount(order.getPayAmount())));
        }

        for (CardPurchaseOrder order : cardPurchases) {
            AdminDashboardStoreMetric metric = resolveStoreMetric(metricMap, storeMap, order.getStoreId());
            metric.setCardPurchaseAmount(metric.getCardPurchaseAmount().add(normalizeAmount(order.getPayAmount())));
        }

        return metricMap.values().stream()
            .sorted(Comparator
                .comparing(this::resolveStoreSortAmount)
                .reversed()
                .thenComparing(AdminDashboardStoreMetric::getCardUsageTimes, Comparator.reverseOrder())
                .thenComparing(AdminDashboardStoreMetric::getStoreId, Comparator.nullsLast(Long::compareTo))
            )
            .limit(8)
            .toList();
    }

    private List<AdminDashboardChannelMetric> buildCardPurchaseChannels(
        List<CardPurchaseOrder> cardPurchases,
        List<UserCard> legacyManualCards,
        List<UserCard> groupBuyVoucherCards
    ) {
        Map<String, AdminDashboardChannelMetric> channelMap = new HashMap<>();
        for (CardPurchaseOrder order : cardPurchases) {
            String channel = StringUtils.hasText(order.getSourceChannel()) ? order.getSourceChannel() : "store";
            AdminDashboardChannelMetric metric = channelMap.computeIfAbsent(channel, key -> {
                AdminDashboardChannelMetric item = new AdminDashboardChannelMetric();
                item.setChannel(key);
                return item;
            });
            metric.setOrderCount(metric.getOrderCount() + 1);
            metric.setCardCount(metric.getCardCount() + normalizeTimes(order.getBuyCount()));
            metric.setPayAmount(metric.getPayAmount().add(normalizeAmount(order.getPayAmount())));
        }

        if (!legacyManualCards.isEmpty()) {
            AdminDashboardChannelMetric metric = channelMap.computeIfAbsent("admin", key -> {
                AdminDashboardChannelMetric item = new AdminDashboardChannelMetric();
                item.setChannel(key);
                return item;
            });
            metric.setOrderCount(metric.getOrderCount() + legacyManualCards.size());
            metric.setCardCount(metric.getCardCount() + legacyManualCards.size());
        }

        for (Map.Entry<String, List<UserCard>> entry : groupVoucherCardsByCode(groupBuyVoucherCards).entrySet()) {
            List<UserCard> cards = entry.getValue();
            if (cards.isEmpty()) {
                continue;
            }
            String channel = normalizeGroupBuySource(cards.get(0).getSourceChannel());
            AdminDashboardChannelMetric metric = channelMap.computeIfAbsent(channel, key -> {
                AdminDashboardChannelMetric item = new AdminDashboardChannelMetric();
                item.setChannel(key);
                return item;
            });
            metric.setOrderCount(metric.getOrderCount() + 1);
            metric.setCardCount(metric.getCardCount() + cards.size());
        }

        return channelMap.values().stream()
            .sorted(Comparator.comparing(AdminDashboardChannelMetric::getPayAmount).reversed())
            .toList();
    }

    private List<AdminDashboardActivityItem> buildRecentActivities(
        List<CardUsageRecord> cardUsages,
        List<WalletTransaction> walletConsumes,
        List<RechargeOrder> rechargeOrders,
        List<CardPurchaseOrder> cardPurchases,
        List<UserCard> legacyManualCards,
        List<UserCard> groupBuyVoucherCards,
        Map<Long, Store> storeMap,
        int limit
    ) {
        List<AdminDashboardActivityItem> activities = new ArrayList<>();

        for (CardUsageRecord usage : cardUsages) {
            activities.add(new AdminDashboardActivityItem(
                "cardUsage",
                "次卡核销",
                resolveStoreName(storeMap, usage.getStoreId()),
                usage.getOrderNo(),
                BigDecimal.ZERO,
                normalizeTimes(usage.getUsedTimes()),
                usage.getUsageTime()
            ));
        }

        for (WalletTransaction tx : walletConsumes) {
            activities.add(new AdminDashboardActivityItem(
                "walletConsume",
                "钱包消费",
                resolveStoreName(storeMap, tx.getStoreId()),
                tx.getRelatedOrderNo(),
                normalizeAmount(tx.getAmount()),
                0L,
                tx.getCreatedAt()
            ));
        }

        for (RechargeOrder order : rechargeOrders) {
            activities.add(new AdminDashboardActivityItem(
                "walletRecharge",
                "钱包充值",
                resolveStoreName(storeMap, order.getStoreId()),
                order.getRechargeOrderNo(),
                normalizeAmount(order.getPayAmount()),
                0L,
                resolveRechargeBizTime(order)
            ));
        }

        for (CardPurchaseOrder order : cardPurchases) {
            activities.add(new AdminDashboardActivityItem(
                "cardPurchase",
                "次卡购买",
                resolveStoreName(storeMap, order.getStoreId()),
                order.getPurchaseOrderNo(),
                normalizeAmount(order.getPayAmount()),
                normalizeTimes(order.getBuyCount()),
                resolveCardPurchaseBizTime(order)
            ));
        }

        for (UserCard card : legacyManualCards) {
            activities.add(new AdminDashboardActivityItem(
                "cardPurchase",
                "后台发卡",
                resolveStoreName(storeMap, card.getStoreId()),
                card.getCardNo(),
                BigDecimal.ZERO,
                1L,
                card.getCreatedAt()
            ));
        }

        for (Map.Entry<String, List<UserCard>> entry : groupVoucherCardsByCode(groupBuyVoucherCards).entrySet()) {
            List<UserCard> cards = entry.getValue();
            if (cards.isEmpty()) {
                continue;
            }
            UserCard first = cards.get(0);
            activities.add(new AdminDashboardActivityItem(
                "groupVerify",
                "团购券核销",
                resolveStoreName(storeMap, first.getStoreId()),
                first.getExternalOrderNo(),
                BigDecimal.ZERO,
                (long) cards.size(),
                first.getCreatedAt()
            ));
        }

        return activities.stream()
            .sorted(Comparator.comparing(AdminDashboardActivityItem::getOccurredAt, Comparator.nullsLast(Comparator.reverseOrder())))
            .limit(limit)
            .toList();
    }

    private int normalizeActivityLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return 100;
        }
        return Math.min(limit, 300);
    }

    private AdminDashboardDeviceStatus buildDeviceStatus(Long storeId) {
        List<Device> devices = deviceMapper.selectList(
            new LambdaQueryWrapper<Device>()
                .eq(storeId != null, Device::getStoreId, storeId)
                .orderByAsc(Device::getStoreId)
                .orderByAsc(Device::getId)
        );

        AdminDashboardDeviceStatus status = new AdminDashboardDeviceStatus();
        status.setTotalCount(devices.size());
        if (devices.isEmpty()) {
            return status;
        }

        Map<Long, Store> storeMap = buildDeviceStoreMap(devices);
        List<AdminDashboardDeviceAlertItem> alerts = new ArrayList<>();

        for (Device device : devices) {
            String deviceStatus = normalizeText(device.getDeviceStatus());
            if ("running".equals(deviceStatus)) {
                status.setRunningCount(status.getRunningCount() + 1);
            } else if ("fault".equals(deviceStatus)) {
                status.setFaultCount(status.getFaultCount() + 1);
            } else if ("offline".equals(deviceStatus)) {
                status.setOfflineCount(status.getOfflineCount() + 1);
            } else if ("disabled".equals(deviceStatus)) {
                status.setDisabledCount(status.getDisabledCount() + 1);
            } else if ("paused".equals(deviceStatus)) {
                status.setPausedCount(status.getPausedCount() + 1);
            } else {
                status.setIdleCount(status.getIdleCount() + 1);
            }

            if (isDeviceAbnormal(deviceStatus)) {
                alerts.add(new AdminDashboardDeviceAlertItem(
                    device.getId(),
                    device.getDeviceCode(),
                    device.getDeviceName(),
                    device.getStoreId(),
                    resolveStoreName(storeMap, device.getStoreId()),
                    deviceStatus,
                    device.getLastHeartbeatTime(),
                    device.getLastOnlineTime(),
                    device.getUpdatedAt(),
                    device.getRemark()
                ));
            }
        }

        long abnormalCount = status.getFaultCount()
            + status.getOfflineCount()
            + status.getDisabledCount()
            + status.getPausedCount();
        status.setAbnormalCount(abnormalCount);
        status.setNormalCount(Math.max(status.getTotalCount() - abnormalCount, 0));
        alerts.sort(Comparator
            .comparingInt((AdminDashboardDeviceAlertItem item) -> deviceSeverityOrder(item.getStatus()))
            .thenComparing(AdminDashboardDeviceAlertItem::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder())));
        status.setAlerts(alerts.stream().limit(12).toList());
        return status;
    }

    private boolean isDeviceAbnormal(String status) {
        return "fault".equals(status)
            || "offline".equals(status)
            || "disabled".equals(status)
            || "paused".equals(status);
    }

    private int deviceSeverityOrder(String status) {
        return switch (normalizeText(status)) {
            case "fault" -> 0;
            case "offline" -> 1;
            case "disabled" -> 2;
            case "paused" -> 3;
            default -> 9;
        };
    }

    private Map<Long, Store> buildDeviceStoreMap(List<Device> devices) {
        Set<Long> storeIds = devices.stream()
            .map(Device::getStoreId)
            .filter(id -> id != null)
            .collect(Collectors.toSet());
        if (storeIds.isEmpty()) {
            return Map.of();
        }
        return storeService.listByIds(storeIds).stream()
            .collect(Collectors.toMap(Store::getId, Function.identity(), (left, right) -> left));
    }

    private Map<Long, Store> buildStoreMap(
        List<CardUsageRecord> cardUsages,
        List<WalletTransaction> walletConsumes,
        List<RechargeOrder> rechargeOrders,
        List<CardPurchaseOrder> cardPurchases,
        List<UserCard> legacyManualCards,
        List<UserCard> groupBuyVoucherCards
    ) {
        Set<Long> storeIds = new HashSet<>();
        cardUsages.stream().map(CardUsageRecord::getStoreId).filter(id -> id != null).forEach(storeIds::add);
        walletConsumes.stream().map(WalletTransaction::getStoreId).filter(id -> id != null).forEach(storeIds::add);
        rechargeOrders.stream().map(RechargeOrder::getStoreId).filter(id -> id != null).forEach(storeIds::add);
        cardPurchases.stream().map(CardPurchaseOrder::getStoreId).filter(id -> id != null).forEach(storeIds::add);
        legacyManualCards.stream().map(UserCard::getStoreId).filter(id -> id != null).forEach(storeIds::add);
        groupBuyVoucherCards.stream().map(UserCard::getStoreId).filter(id -> id != null).forEach(storeIds::add);

        if (storeIds.isEmpty()) {
            return Map.of();
        }

        return storeService.listByIds(storeIds).stream()
            .collect(Collectors.toMap(Store::getId, Function.identity(), (left, right) -> left));
    }

    private AdminDashboardStoreMetric resolveStoreMetric(
        Map<Long, AdminDashboardStoreMetric> metricMap,
        Map<Long, Store> storeMap,
        Long storeId
    ) {
        Long key = storeId != null ? storeId : 0L;
        return metricMap.computeIfAbsent(key, id -> {
            AdminDashboardStoreMetric metric = new AdminDashboardStoreMetric();
            metric.setStoreId(storeId);
            metric.setStoreName(resolveStoreName(storeMap, storeId));
            return metric;
        });
    }

    private String resolveStoreName(Map<Long, Store> storeMap, Long storeId) {
        if (storeId == null) {
            return "未知门店";
        }
        Store store = storeMap.get(storeId);
        return store != null && StringUtils.hasText(store.getStoreName()) ? store.getStoreName() : "门店" + storeId;
    }

    private BigDecimal resolveStoreSortAmount(AdminDashboardStoreMetric metric) {
        return metric.getWalletConsumeAmount()
            .add(metric.getWalletRechargeAmount())
            .add(metric.getCardPurchaseAmount());
    }

    private long countDistinctWalletActions(List<WalletTransaction> transactions) {
        return transactions.stream()
            .map(tx -> {
                if (tx.getRelatedOrderId() != null) {
                    return "order:" + tx.getRelatedOrderId();
                }
                if (StringUtils.hasText(tx.getRelatedOrderNo())) {
                    return "orderNo:" + tx.getRelatedOrderNo();
                }
                return "tx:" + tx.getId();
            })
            .distinct()
            .count();
    }

    private long countDistinctVoucherCodes(List<UserCard> cards) {
        return groupVoucherCardsByCode(cards).size();
    }

    private List<UserCard> buildDistinctVoucherCards(List<UserCard> cards) {
        return groupVoucherCardsByCode(cards).values().stream()
            .map(group -> group.get(0))
            .toList();
    }

    private Map<String, List<UserCard>> groupVoucherCardsByCode(List<UserCard> cards) {
        if (cards == null || cards.isEmpty()) {
            return Map.of();
        }
        Map<String, List<UserCard>> grouped = new LinkedHashMap<>();
        for (UserCard card : cards) {
            String externalOrderNo = StringUtils.hasText(card.getExternalOrderNo())
                ? card.getExternalOrderNo().trim()
                : "CARD_" + card.getId();
            grouped.computeIfAbsent(externalOrderNo, ignored -> new ArrayList<>()).add(card);
        }
        return grouped;
    }

    private String normalizeGroupBuySource(String sourceChannel) {
        String source = normalizeText(sourceChannel);
        return GROUP_BUY_CHANNELS.contains(source) ? source : "douyin";
    }

    private Map<Long, UserCard> buildUsageCardMap(List<CardUsageRecord> usages) {
        List<Long> cardIds = usages.stream()
            .map(CardUsageRecord::getUserCardId)
            .filter(id -> id != null)
            .distinct()
            .toList();

        if (cardIds.isEmpty()) {
            return Map.of();
        }

        return userCardMapper.selectBatchIds(cardIds).stream()
            .collect(Collectors.toMap(UserCard::getId, Function.identity(), (left, right) -> left));
    }

    private List<CardUsageRecord> filterGroupBuyUsages(List<CardUsageRecord> usages, Map<Long, UserCard> userCardMap) {
        return usages.stream()
            .filter(usage -> {
                UserCard card = userCardMap.get(usage.getUserCardId());
                if (card == null || !StringUtils.hasText(card.getSourceChannel())) {
                    return false;
                }
                return GROUP_BUY_CHANNELS.contains(card.getSourceChannel().trim().toLowerCase());
            })
            .toList();
    }

    private long countWashOrders(List<WashOrder> orders) {
        return orders.stream().filter(this::isWashOrderCounted).count();
    }

    private boolean isWashOrderCounted(WashOrder order) {
        if (order == null) {
            return false;
        }
        String status = normalizeText(order.getOrderStatus());
        if ("cancelled".equals(status) || "canceled".equals(status) || "initial".equals(status)) {
            return false;
        }
        return order.getStartTime() != null
            || "running".equals(status)
            || "completed".equals(status)
            || "paid".equals(normalizeText(order.getPaymentStatus()));
    }

    private long countWalletWashOrders(List<WashOrder> orders) {
        return orders.stream()
            .filter(this::isWashOrderCounted)
            .filter(order -> "wallet".equals(normalizeText(order.getPayMode())))
            .count();
    }

    private BigDecimal sumWalletTransactionAmount(List<WalletTransaction> transactions) {
        return transactions.stream()
            .map(tx -> normalizeAmount(tx.getAmount()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumRechargePayAmount(List<RechargeOrder> orders) {
        return orders.stream()
            .map(order -> normalizeAmount(order.getPayAmount()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private WalletBalanceSnapshot buildWalletBalanceSnapshot(Long storeId) {
        BigDecimal principalBalance = BigDecimal.ZERO;
        BigDecimal giftBalance = BigDecimal.ZERO;
        for (UserStoreWallet wallet : userStoreWalletMapper.selectList(
            new LambdaQueryWrapper<UserStoreWallet>()
                .eq(storeId != null, UserStoreWallet::getStoreId, storeId)
        )) {
            principalBalance = principalBalance.add(normalizeAmount(wallet.getPrincipalBalance()));
            giftBalance = giftBalance.add(normalizeAmount(wallet.getGiftBalance()));
        }
        return new WalletBalanceSnapshot(principalBalance, giftBalance);
    }

    private long countUsers(Long storeId) {
        if (storeId != null) {
            Long count = userStoreWalletMapper.selectCount(
                new LambdaQueryWrapper<UserStoreWallet>()
                    .eq(UserStoreWallet::getStoreId, storeId)
            );
            return count != null ? count : 0L;
        }
        Long count = userInfoMapper.selectCount(new LambdaQueryWrapper<UserInfo>());
        return count != null ? count : 0L;
    }

    private long countNewUsers(LocalDateTime start, LocalDateTime end, Long storeId) {
        if (storeId != null) {
            Long count = userStoreWalletMapper.selectCount(
                new LambdaQueryWrapper<UserStoreWallet>()
                    .eq(UserStoreWallet::getStoreId, storeId)
                    .ge(start != null, UserStoreWallet::getCreatedAt, start)
                    .lt(end != null, UserStoreWallet::getCreatedAt, end)
            );
            return count != null ? count : 0L;
        }
        Long count = userInfoMapper.selectCount(
            new LambdaQueryWrapper<UserInfo>()
                .ge(start != null, UserInfo::getCreatedAt, start)
                .lt(end != null, UserInfo::getCreatedAt, end)
        );
        return count != null ? count : 0L;
    }

    private Long normalizeStoreId(Long storeId) {
        return storeId != null && storeId > 0 ? storeId : null;
    }

    private boolean isMonthlyCardPurchase(CardPurchaseOrder order) {
        if (order == null) {
            return false;
        }
        String cardType = normalizeText(order.getCardType());
        return cardType.contains("month") || cardType.contains("monthly") || cardType.contains("月");
    }

    private LocalDate resolveOrderDate(WashOrder order) {
        LocalDateTime time = order.getStartTime() != null ? order.getStartTime() : order.getCreatedAt();
        return resolveDate(time);
    }

    private LocalDateTime resolveRechargeBizTime(RechargeOrder order) {
        if (order == null) {
            return null;
        }
        return order.getPayTime() != null ? order.getPayTime() : order.getCreatedAt();
    }

    private LocalDateTime resolveCardPurchaseBizTime(CardPurchaseOrder order) {
        if (order == null) {
            return null;
        }
        return order.getPurchaseTime() != null ? order.getPurchaseTime() : order.getCreatedAt();
    }

    private LocalDate resolveDate(LocalDateTime time) {
        return time != null ? time.toLocalDate() : LocalDate.now();
    }

    private int resolveHour(LocalDateTime time) {
        return time != null ? time.getHour() : 0;
    }

    private BigDecimal normalizeAmount(BigDecimal amount) {
        return amount != null ? amount : BigDecimal.ZERO;
    }

    private long normalizeTimes(Integer value) {
        return value != null ? Math.max(value, 0) : 0L;
    }

    private String normalizeText(String value) {
        return StringUtils.hasText(value) ? value.trim().toLowerCase() : "";
    }

    private record WalletBalanceSnapshot(BigDecimal principalBalance, BigDecimal giftBalance) {

        BigDecimal totalBalance() {
            return principalBalance.add(giftBalance);
        }
    }
}
