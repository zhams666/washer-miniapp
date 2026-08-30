package com.washer.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.washer.backend.dto.device.DeviceSimpleItem;
import com.washer.backend.dto.miniadmin.MiniAdminDashboardOverview;
import com.washer.backend.dto.miniadmin.MiniAdminDeviceAlertItem;
import com.washer.backend.dto.miniadmin.MiniAdminDeviceStatusSummary;
import com.washer.backend.dto.miniadmin.MiniAdminMetricItem;
import com.washer.backend.dto.miniadmin.MiniAdminOrderItem;
import com.washer.backend.dto.miniadmin.MiniAdminOperationOverview;
import com.washer.backend.dto.miniadmin.MiniAdminRecentActivityItem;
import com.washer.backend.dto.miniadmin.MiniAdminScopeSummaryItem;
import com.washer.backend.dto.miniadmin.MiniAdminSessionContext;
import com.washer.backend.dto.miniadmin.MiniAdminStoreOption;
import com.washer.backend.dto.miniadmin.MiniAdminStoreRankingItem;
import com.washer.backend.entity.CardUsageRecord;
import com.washer.backend.entity.Device;
import com.washer.backend.entity.Franchisee;
import com.washer.backend.entity.RechargeOrder;
import com.washer.backend.entity.Store;
import com.washer.backend.entity.WalletTransaction;
import com.washer.backend.entity.WashOrder;
import com.washer.backend.mapper.CardUsageRecordMapper;
import com.washer.backend.mapper.DeviceMapper;
import com.washer.backend.mapper.FranchiseeMapper;
import com.washer.backend.mapper.MiniAdminStaffMapper;
import com.washer.backend.mapper.RechargeOrderMapper;
import com.washer.backend.mapper.StoreMapper;
import com.washer.backend.mapper.WalletTransactionMapper;
import com.washer.backend.mapper.WashOrderMapper;
import com.washer.backend.service.DeviceService;
import com.washer.backend.service.MiniAdminPortalService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class MiniAdminPortalServiceImpl implements MiniAdminPortalService {

    private static final String PERMISSION_DEVICE_CONTROL = "device:control";
    private static final String ROLE_PLATFORM_ADMIN = "platform_admin";
    private static final String ROLE_FRANCHISEE_OWNER = "franchisee_owner";
    private static final String ROLE_STORE_MANAGER = "store_manager";

    private final StoreMapper storeMapper;
    private final DeviceMapper deviceMapper;
    private final WashOrderMapper washOrderMapper;
    private final WalletTransactionMapper walletTransactionMapper;
    private final RechargeOrderMapper rechargeOrderMapper;
    private final CardUsageRecordMapper cardUsageRecordMapper;
    private final MiniAdminStaffMapper miniAdminStaffMapper;
    private final FranchiseeMapper franchiseeMapper;
    private final DeviceService deviceService;

    public MiniAdminPortalServiceImpl(
        StoreMapper storeMapper,
        DeviceMapper deviceMapper,
        WashOrderMapper washOrderMapper,
        WalletTransactionMapper walletTransactionMapper,
        RechargeOrderMapper rechargeOrderMapper,
        CardUsageRecordMapper cardUsageRecordMapper,
        MiniAdminStaffMapper miniAdminStaffMapper,
        FranchiseeMapper franchiseeMapper,
        DeviceService deviceService
    ) {
        this.storeMapper = storeMapper;
        this.deviceMapper = deviceMapper;
        this.washOrderMapper = washOrderMapper;
        this.walletTransactionMapper = walletTransactionMapper;
        this.rechargeOrderMapper = rechargeOrderMapper;
        this.cardUsageRecordMapper = cardUsageRecordMapper;
        this.miniAdminStaffMapper = miniAdminStaffMapper;
        this.franchiseeMapper = franchiseeMapper;
        this.deviceService = deviceService;
    }

    @Override
    public MiniAdminDashboardOverview getDashboard(MiniAdminSessionContext context, LocalDate bizDate, Long storeId) {
        StoreScope scope = resolveScope(context, storeId);
        LocalDate date = bizDate != null ? bizDate : LocalDate.now();
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();

        if (scope.isEmpty()) {
            MiniAdminDeviceStatusSummary emptyDeviceSummary = new MiniAdminDeviceStatusSummary();
            return new MiniAdminDashboardOverview(
                date,
                LocalDateTime.now(),
                null,
                context.getStores(),
                buildMetrics(0, BigDecimal.ZERO, BigDecimal.ZERO, 0L),
                emptyDeviceSummary,
                List.of()
            );
        }

        List<WashOrder> washOrders = loadWashOrders(scope, start, end);
        List<WalletTransaction> consumes = loadWalletConsumes(scope, start, end);
        List<RechargeOrder> recharges = loadRecharges(scope, start, end);
        List<CardUsageRecord> cardUsages = loadCardUsages(scope, start, end);
        List<Device> devices = loadDevices(scope, null);
        Map<Long, Store> storeMap = buildStoreMap(scope);

        long washCount = washOrders.stream().filter(this::isCountedWashOrder).count();
        BigDecimal consumeAmount = consumes.stream()
            .map(tx -> normalizeAmount(tx.getAmount()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal rechargeAmount = recharges.stream()
            .map(order -> normalizeAmount(order.getPayAmount()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        long cardUsageTimes = cardUsages.stream()
            .map(CardUsageRecord::getUsedTimes)
            .filter(times -> times != null && times > 0)
            .mapToLong(Integer::longValue)
            .sum();

        return new MiniAdminDashboardOverview(
            date,
            LocalDateTime.now(),
            resolveActiveStore(scope),
            context.getStores(),
            buildMetrics(washCount, consumeAmount, rechargeAmount, cardUsageTimes),
            buildDeviceStatus(devices, storeMap),
            buildRecentActivities(scope, storeMap)
        );
    }

    @Override
    public MiniAdminOperationOverview getOperationOverview(MiniAdminSessionContext context, LocalDate bizDate, Long storeId) {
        StoreScope scope = resolveScope(context, storeId);
        LocalDate date = bizDate != null ? bizDate : LocalDate.now();
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();
        String tierCode = resolveTierCode(context);

        if (scope.isEmpty()) {
            MiniAdminDeviceStatusSummary emptyDeviceSummary = new MiniAdminDeviceStatusSummary();
            return new MiniAdminOperationOverview(
                tierCode,
                resolveTierName(tierCode),
                resolveHeadline(tierCode),
                resolveDescription(tierCode),
                resolveScopeName(context, List.of()),
                date,
                LocalDateTime.now(),
                null,
                context.getStores(),
                buildScopeSummary(context, List.of(), 0L),
                buildMetrics(0, BigDecimal.ZERO, BigDecimal.ZERO, 0L),
                emptyDeviceSummary,
                List.of(),
                List.of()
            );
        }

        Map<Long, Store> storeMap = buildStoreMap(scope);
        List<Store> scopeStores = new ArrayList<>(storeMap.values());
        List<WashOrder> washOrders = loadWashOrders(scope, start, end);
        List<WalletTransaction> consumes = loadWalletConsumes(scope, start, end);
        List<RechargeOrder> recharges = loadRecharges(scope, start, end);
        List<CardUsageRecord> cardUsages = loadCardUsages(scope, start, end);
        List<Device> devices = loadDevices(scope, null);

        long washCount = washOrders.stream().filter(this::isCountedWashOrder).count();
        BigDecimal consumeAmount = consumes.stream()
            .map(tx -> normalizeAmount(tx.getAmount()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal rechargeAmount = recharges.stream()
            .map(order -> normalizeAmount(order.getPayAmount()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        long cardUsageTimes = cardUsages.stream()
            .map(CardUsageRecord::getUsedTimes)
            .filter(times -> times != null && times > 0)
            .mapToLong(Integer::longValue)
            .sum();

        return new MiniAdminOperationOverview(
            tierCode,
            resolveTierName(tierCode),
            resolveHeadline(tierCode),
            resolveDescription(tierCode),
            resolveScopeName(context, scopeStores),
            date,
            LocalDateTime.now(),
            resolveActiveStore(scope),
            context.getStores(),
            buildScopeSummary(context, scopeStores, countVisibleStaff(context)),
            buildMetrics(washCount, consumeAmount, rechargeAmount, cardUsageTimes),
            buildDeviceStatus(devices, storeMap),
            buildStoreRankings(scopeStores, washOrders, consumes, recharges, devices),
            buildRecentActivities(scope, storeMap)
        );
    }

    @Override
    public List<DeviceSimpleItem> listDevices(MiniAdminSessionContext context, Long storeId, String keyword) {
        StoreScope scope = resolveScope(context, storeId);
        if (scope.isEmpty()) {
            return List.of();
        }
        List<Device> devices = loadDevices(scope, keyword);
        Map<Long, Store> storeMap = buildStoreMapFromIds(devices.stream().map(Device::getStoreId).toList());
        return devices.stream()
            .map(device -> toDeviceItem(device, storeMap))
            .toList();
    }

    @Override
    public DeviceSimpleItem startDevice(MiniAdminSessionContext context, Long deviceId) {
        ensureDeviceControlPermission(context);
        Device device = getAccessibleDevice(context, deviceId);
        return deviceService.mockStartDevice(device.getId());
    }

    @Override
    public DeviceSimpleItem stopDevice(MiniAdminSessionContext context, Long deviceId) {
        ensureDeviceControlPermission(context);
        Device device = getAccessibleDevice(context, deviceId);
        return deviceService.mockStopDevice(device.getId());
    }

    @Override
    public Page<MiniAdminOrderItem> pageOrders(
        MiniAdminSessionContext context,
        long page,
        long size,
        Long storeId,
        String orderStatus,
        String paymentStatus,
        String keyword
    ) {
        StoreScope scope = resolveScope(context, storeId);
        if (scope.isEmpty()) {
            return new Page<>(page, size, 0);
        }

        LambdaQueryWrapper<WashOrder> wrapper = new LambdaQueryWrapper<WashOrder>()
            .eq(scope.singleStoreId() != null, WashOrder::getStoreId, scope.singleStoreId())
            .in(scope.needsInScope(), WashOrder::getStoreId, scope.storeIds())
            .eq(StringUtils.hasText(orderStatus), WashOrder::getOrderStatus, orderStatus)
            .eq(StringUtils.hasText(paymentStatus), WashOrder::getPaymentStatus, paymentStatus)
            .orderByDesc(WashOrder::getId);

        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w
                .like(WashOrder::getOrderNo, keyword)
                .or()
                .like(WashOrder::getRemark, keyword));
        }

        Page<WashOrder> orderPage = washOrderMapper.selectPage(new Page<>(page, size), wrapper);
        Map<Long, Store> storeMap = buildStoreMapFromIds(orderPage.getRecords().stream().map(WashOrder::getStoreId).toList());
        Map<Long, Device> deviceMap = buildDeviceMap(orderPage.getRecords());

        Page<MiniAdminOrderItem> result = new Page<>(orderPage.getCurrent(), orderPage.getSize(), orderPage.getTotal());
        result.setRecords(orderPage.getRecords().stream()
            .map(order -> toOrderItem(order, storeMap, deviceMap))
            .toList());
        return result;
    }

    private String resolveTierCode(MiniAdminSessionContext context) {
        String role = normalizeStatus(context.getStaff().getRoleCode());
        if (ROLE_PLATFORM_ADMIN.equals(role) || context.isPlatformScope()) {
            return "platform";
        }
        if (ROLE_FRANCHISEE_OWNER.equals(role) || "franchisee".equals(normalizeStatus(context.getStaff().getDataScope()))) {
            return "franchisee";
        }
        return "store";
    }

    private String resolveTierName(String tierCode) {
        return switch (normalizeStatus(tierCode)) {
            case "platform" -> "总部";
            case "franchisee" -> "加盟老板";
            default -> "门店店长";
        };
    }

    private String resolveHeadline(String tierCode) {
        return switch (normalizeStatus(tierCode)) {
            case "platform" -> "总部经营驾驶舱";
            case "franchisee" -> "加盟老板驾驶舱";
            default -> "门店移动工作台";
        };
    }

    private String resolveDescription(String tierCode) {
        return switch (normalizeStatus(tierCode)) {
            case "platform" -> "查看全部加盟商、全部门店和全局经营风险。";
            case "franchisee" -> "查看旗下门店经营、设备异常和门店排行。";
            default -> "聚焦本店设备、订单、流水和现场处理。";
        };
    }

    private String resolveScopeName(MiniAdminSessionContext context, List<Store> stores) {
        String tierCode = resolveTierCode(context);
        if ("platform".equals(tierCode)) {
            return "全部加盟商";
        }
        if ("franchisee".equals(tierCode)) {
            Franchisee franchisee = context.getStaff().getFranchiseeId() != null
                ? franchiseeMapper.selectById(context.getStaff().getFranchiseeId())
                : null;
            return franchisee != null && StringUtils.hasText(franchisee.getFranchiseeName())
                ? franchisee.getFranchiseeName()
                : "当前加盟商";
        }
        if (stores.size() == 1) {
            return stores.get(0).getStoreName();
        }
        return "已绑定门店";
    }

    private List<MiniAdminScopeSummaryItem> buildScopeSummary(
        MiniAdminSessionContext context,
        List<Store> stores,
        long staffCount
    ) {
        String tierCode = resolveTierCode(context);
        long storeCount = stores.size();
        long franchiseeCount = countVisibleFranchisees(context, stores);
        long deviceCount = stores.isEmpty() ? 0L : deviceMapper.selectCount(
            new LambdaQueryWrapper<Device>()
                .in(Device::getStoreId, stores.stream().map(Store::getId).distinct().toList())
        );
        long abnormalDeviceCount = stores.isEmpty() ? 0L : deviceMapper.selectList(
            new LambdaQueryWrapper<Device>()
                .in(Device::getStoreId, stores.stream().map(Store::getId).distinct().toList())
        ).stream().filter(device -> isAbnormal(normalizeStatus(device.getDeviceStatus()))).count();

        if ("platform".equals(tierCode)) {
            return List.of(
                new MiniAdminScopeSummaryItem("franchisees", "加盟商", franchiseeCount, "个", "总部可见的加盟主体"),
                new MiniAdminScopeSummaryItem("stores", "门店", storeCount, "家", "全部直营网点和加盟门店"),
                new MiniAdminScopeSummaryItem("staff", "管理账号", staffCount, "个", "管理端已绑定账号"),
                new MiniAdminScopeSummaryItem("devices", "设备", deviceCount, "台", "全部门店设备")
            );
        }
        if ("franchisee".equals(tierCode)) {
            return List.of(
                new MiniAdminScopeSummaryItem("stores", "旗下门店", storeCount, "家", "当前加盟商门店"),
                new MiniAdminScopeSummaryItem("staff", "管理账号", staffCount, "个", "加盟商体系管理账号"),
                new MiniAdminScopeSummaryItem("devices", "设备", deviceCount, "台", "旗下门店设备"),
                new MiniAdminScopeSummaryItem("abnormalDevices", "异常设备", abnormalDeviceCount, "台", "需优先处理")
            );
        }
        return List.of(
            new MiniAdminScopeSummaryItem("stores", "可管门店", storeCount, "家", "当前店长绑定范围"),
            new MiniAdminScopeSummaryItem("devices", "设备", deviceCount, "台", "本店或绑定门店设备"),
            new MiniAdminScopeSummaryItem("abnormalDevices", "异常设备", abnormalDeviceCount, "台", "现场需处理"),
            new MiniAdminScopeSummaryItem("staff", "管理账号", staffCount, "个", "同加盟主体账号")
        );
    }

    private long countVisibleFranchisees(MiniAdminSessionContext context, List<Store> stores) {
        if (context.isPlatformScope()) {
            return franchiseeMapper.selectCount(new LambdaQueryWrapper<Franchisee>());
        }
        return stores.stream()
            .map(Store::getFranchiseeId)
            .filter(id -> id != null)
            .distinct()
            .count();
    }

    private long countVisibleStaff(MiniAdminSessionContext context) {
        if (context.isPlatformScope()) {
            return miniAdminStaffMapper.selectCount(new LambdaQueryWrapper<>());
        }
        Long franchiseeId = context.getStaff().getFranchiseeId();
        if (franchiseeId == null) {
            return 0L;
        }
        return miniAdminStaffMapper.selectCount(
            new LambdaQueryWrapper<com.washer.backend.entity.MiniAdminStaff>()
                .eq(com.washer.backend.entity.MiniAdminStaff::getFranchiseeId, franchiseeId)
        );
    }

    private List<MiniAdminStoreRankingItem> buildStoreRankings(
        List<Store> stores,
        List<WashOrder> washOrders,
        List<WalletTransaction> consumes,
        List<RechargeOrder> recharges,
        List<Device> devices
    ) {
        Map<Long, Franchisee> franchiseeMap = buildFranchiseeMap(stores);
        Map<Long, Long> washCountMap = washOrders.stream()
            .filter(this::isCountedWashOrder)
            .filter(order -> order.getStoreId() != null)
            .collect(Collectors.groupingBy(WashOrder::getStoreId, Collectors.counting()));
        Map<Long, BigDecimal> consumeMap = consumes.stream()
            .filter(tx -> tx.getStoreId() != null)
            .collect(Collectors.groupingBy(
                WalletTransaction::getStoreId,
                Collectors.mapping(tx -> normalizeAmount(tx.getAmount()), Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))
            ));
        Map<Long, BigDecimal> rechargeMap = recharges.stream()
            .filter(order -> order.getStoreId() != null)
            .collect(Collectors.groupingBy(
                RechargeOrder::getStoreId,
                Collectors.mapping(order -> normalizeAmount(order.getPayAmount()), Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))
            ));
        Map<Long, Long> deviceCountMap = devices.stream()
            .filter(device -> device.getStoreId() != null)
            .collect(Collectors.groupingBy(Device::getStoreId, Collectors.counting()));
        Map<Long, Long> abnormalCountMap = devices.stream()
            .filter(device -> device.getStoreId() != null)
            .filter(device -> isAbnormal(normalizeStatus(device.getDeviceStatus())))
            .collect(Collectors.groupingBy(Device::getStoreId, Collectors.counting()));

        return stores.stream()
            .map(store -> {
                Franchisee franchisee = franchiseeMap.get(store.getFranchiseeId());
                return new MiniAdminStoreRankingItem(
                    store.getId(),
                    store.getFranchiseeId(),
                    store.getStoreName(),
                    franchisee != null ? franchisee.getFranchiseeName() : "",
                    washCountMap.getOrDefault(store.getId(), 0L),
                    consumeMap.getOrDefault(store.getId(), BigDecimal.ZERO),
                    rechargeMap.getOrDefault(store.getId(), BigDecimal.ZERO),
                    abnormalCountMap.getOrDefault(store.getId(), 0L),
                    deviceCountMap.getOrDefault(store.getId(), 0L)
                );
            })
            .sorted(Comparator
                .comparing(MiniAdminStoreRankingItem::getConsumeAmount, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(MiniAdminStoreRankingItem::getWashCount, Comparator.nullsLast(Comparator.reverseOrder()))
            )
            .limit(10)
            .toList();
    }

    private Map<Long, Franchisee> buildFranchiseeMap(List<Store> stores) {
        List<Long> franchiseeIds = stores.stream()
            .map(Store::getFranchiseeId)
            .filter(id -> id != null)
            .distinct()
            .toList();
        if (franchiseeIds.isEmpty()) {
            return Map.of();
        }
        return franchiseeMapper.selectBatchIds(franchiseeIds).stream()
            .collect(Collectors.toMap(Franchisee::getId, Function.identity(), (left, right) -> left));
    }

    private StoreScope resolveScope(MiniAdminSessionContext context, Long requestedStoreId) {
        if (context == null) {
            throw new IllegalArgumentException("管理端登录已失效，请重新登录");
        }
        if (requestedStoreId != null && requestedStoreId > 0) {
            if (!context.isPlatformScope()
                && context.getStores().stream().noneMatch(store -> requestedStoreId.equals(store.getId()))) {
                throw new IllegalArgumentException("无权访问该门店");
            }
            return StoreScope.single(requestedStoreId);
        }
        if (context.isPlatformScope()) {
            return StoreScope.unlimited();
        }
        List<Long> storeIds = context.getStores().stream().map(MiniAdminStoreOption::getId).distinct().toList();
        return StoreScope.limited(storeIds);
    }

    private List<WashOrder> loadWashOrders(StoreScope scope, LocalDateTime start, LocalDateTime end) {
        return washOrderMapper.selectList(
            new LambdaQueryWrapper<WashOrder>()
                .eq(scope.singleStoreId() != null, WashOrder::getStoreId, scope.singleStoreId())
                .in(scope.needsInScope(), WashOrder::getStoreId, scope.storeIds())
                .ge(WashOrder::getCreatedAt, start)
                .lt(WashOrder::getCreatedAt, end)
                .orderByDesc(WashOrder::getId)
        );
    }

    private List<WalletTransaction> loadWalletConsumes(StoreScope scope, LocalDateTime start, LocalDateTime end) {
        return walletTransactionMapper.selectList(
            new LambdaQueryWrapper<WalletTransaction>()
                .eq(scope.singleStoreId() != null, WalletTransaction::getStoreId, scope.singleStoreId())
                .in(scope.needsInScope(), WalletTransaction::getStoreId, scope.storeIds())
                .eq(WalletTransaction::getBizType, "consume")
                .eq(WalletTransaction::getChangeType, "out")
                .ge(WalletTransaction::getCreatedAt, start)
                .lt(WalletTransaction::getCreatedAt, end)
                .orderByDesc(WalletTransaction::getId)
        );
    }

    private List<RechargeOrder> loadRecharges(StoreScope scope, LocalDateTime start, LocalDateTime end) {
        return rechargeOrderMapper.selectList(
            new LambdaQueryWrapper<RechargeOrder>()
                .eq(scope.singleStoreId() != null, RechargeOrder::getStoreId, scope.singleStoreId())
                .in(scope.needsInScope(), RechargeOrder::getStoreId, scope.storeIds())
                .eq(RechargeOrder::getPayStatus, "paid")
                .ge(RechargeOrder::getCreatedAt, start)
                .lt(RechargeOrder::getCreatedAt, end)
                .orderByDesc(RechargeOrder::getId)
        );
    }

    private List<CardUsageRecord> loadCardUsages(StoreScope scope, LocalDateTime start, LocalDateTime end) {
        return cardUsageRecordMapper.selectList(
            new LambdaQueryWrapper<CardUsageRecord>()
                .eq(scope.singleStoreId() != null, CardUsageRecord::getStoreId, scope.singleStoreId())
                .in(scope.needsInScope(), CardUsageRecord::getStoreId, scope.storeIds())
                .ge(CardUsageRecord::getUsageTime, start)
                .lt(CardUsageRecord::getUsageTime, end)
                .orderByDesc(CardUsageRecord::getId)
        );
    }

    private List<Device> loadDevices(StoreScope scope, String keyword) {
        LambdaQueryWrapper<Device> wrapper = new LambdaQueryWrapper<Device>()
            .eq(scope.singleStoreId() != null, Device::getStoreId, scope.singleStoreId())
            .in(scope.needsInScope(), Device::getStoreId, scope.storeIds())
            .orderByAsc(Device::getStoreId)
            .orderByAsc(Device::getId);

        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w
                .like(Device::getDeviceCode, keyword)
                .or()
                .like(Device::getDeviceName, keyword));
        }
        return deviceMapper.selectList(wrapper);
    }

    private List<MiniAdminMetricItem> buildMetrics(
        long washCount,
        BigDecimal consumeAmount,
        BigDecimal rechargeAmount,
        long cardUsageTimes
    ) {
        return List.of(
            new MiniAdminMetricItem("washCount", "今日洗车", BigDecimal.ZERO, washCount, "辆", "当前权限范围内订单"),
            new MiniAdminMetricItem("consumeAmount", "今日消费", consumeAmount, 0L, "元", "钱包消费流水"),
            new MiniAdminMetricItem("rechargeAmount", "今日充值", rechargeAmount, 0L, "元", "已支付充值订单"),
            new MiniAdminMetricItem("cardUsageTimes", "次卡核销", BigDecimal.ZERO, cardUsageTimes, "次", "今日次卡使用次数")
        );
    }

    private MiniAdminDeviceStatusSummary buildDeviceStatus(List<Device> devices, Map<Long, Store> storeMap) {
        MiniAdminDeviceStatusSummary summary = new MiniAdminDeviceStatusSummary();
        summary.setTotalCount(devices.size());
        List<MiniAdminDeviceAlertItem> alerts = new ArrayList<>();
        for (Device device : devices) {
            String status = normalizeStatus(device.getDeviceStatus());
            switch (status) {
                case "running" -> summary.setRunningCount(summary.getRunningCount() + 1);
                case "fault" -> summary.setFaultCount(summary.getFaultCount() + 1);
                case "offline" -> summary.setOfflineCount(summary.getOfflineCount() + 1);
                case "disabled" -> summary.setDisabledCount(summary.getDisabledCount() + 1);
                case "paused" -> summary.setPausedCount(summary.getPausedCount() + 1);
                default -> summary.setIdleCount(summary.getIdleCount() + 1);
            }
            if (isAbnormal(status)) {
                alerts.add(new MiniAdminDeviceAlertItem(
                    device.getId(),
                    device.getDeviceCode(),
                    device.getDeviceName(),
                    device.getStoreId(),
                    resolveStoreName(storeMap, device.getStoreId()),
                    status,
                    device.getLastHeartbeatTime(),
                    device.getUpdatedAt()
                ));
            }
        }
        summary.setAbnormalCount(
            summary.getFaultCount() + summary.getOfflineCount() + summary.getDisabledCount() + summary.getPausedCount()
        );
        alerts.sort(Comparator
            .comparingInt((MiniAdminDeviceAlertItem item) -> deviceSeverity(item.getStatus()))
            .thenComparing(MiniAdminDeviceAlertItem::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder())));
        summary.setAlerts(alerts.stream().limit(8).toList());
        return summary;
    }

    private List<MiniAdminRecentActivityItem> buildRecentActivities(StoreScope scope, Map<Long, Store> storeMap) {
        List<MiniAdminRecentActivityItem> activities = new ArrayList<>();
        for (WashOrder order : washOrderMapper.selectList(
            new LambdaQueryWrapper<WashOrder>()
                .eq(scope.singleStoreId() != null, WashOrder::getStoreId, scope.singleStoreId())
                .in(scope.needsInScope(), WashOrder::getStoreId, scope.storeIds())
                .orderByDesc(WashOrder::getId)
                .last("limit 8")
        )) {
            activities.add(new MiniAdminRecentActivityItem(
                "order",
                "洗车订单",
                order.getStoreId(),
                resolveStoreName(storeMap, order.getStoreId()),
                order.getOrderNo(),
                normalizeAmount(order.getFinalAmount()),
                0L,
                order.getCreatedAt()
            ));
        }
        for (WalletTransaction tx : walletTransactionMapper.selectList(
            new LambdaQueryWrapper<WalletTransaction>()
                .eq(scope.singleStoreId() != null, WalletTransaction::getStoreId, scope.singleStoreId())
                .in(scope.needsInScope(), WalletTransaction::getStoreId, scope.storeIds())
                .orderByDesc(WalletTransaction::getId)
                .last("limit 8")
        )) {
            activities.add(new MiniAdminRecentActivityItem(
                "wallet",
                formatWalletActivityTitle(tx.getBizType()),
                tx.getStoreId(),
                resolveStoreName(storeMap, tx.getStoreId()),
                tx.getRelatedOrderNo() != null ? tx.getRelatedOrderNo() : tx.getTransactionNo(),
                normalizeAmount(tx.getAmount()),
                0L,
                tx.getCreatedAt()
            ));
        }
        return activities.stream()
            .sorted(Comparator.comparing(MiniAdminRecentActivityItem::getOccurredAt, Comparator.nullsLast(Comparator.reverseOrder())))
            .limit(10)
            .toList();
    }

    private Device getAccessibleDevice(MiniAdminSessionContext context, Long deviceId) {
        if (deviceId == null) {
            throw new IllegalArgumentException("deviceId is required");
        }
        Device device = deviceMapper.selectById(deviceId);
        if (device == null) {
            throw new IllegalArgumentException("device not found");
        }
        if (!context.isPlatformScope()
            && context.getStores().stream().noneMatch(store -> device.getStoreId() != null && device.getStoreId().equals(store.getId()))) {
            throw new IllegalArgumentException("无权操作该设备");
        }
        return device;
    }

    private void ensureDeviceControlPermission(MiniAdminSessionContext context) {
        if (context == null || !context.getPermissions().contains(PERMISSION_DEVICE_CONTROL)) {
            throw new IllegalArgumentException("无设备控制权限");
        }
    }

    private Map<Long, Store> buildStoreMap(StoreScope scope) {
        if (scope.allStores()) {
            return storeMapper.selectList(new LambdaQueryWrapper<Store>())
                .stream()
                .collect(Collectors.toMap(Store::getId, Function.identity(), (left, right) -> left, LinkedHashMap::new));
        }
        return buildStoreMapFromIds(scope.storeIds());
    }

    private Map<Long, Store> buildStoreMapFromIds(List<Long> storeIds) {
        List<Long> ids = storeIds.stream().filter(id -> id != null).distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return storeMapper.selectBatchIds(ids)
            .stream()
            .collect(Collectors.toMap(Store::getId, Function.identity(), (left, right) -> left, LinkedHashMap::new));
    }

    private Map<Long, Device> buildDeviceMap(List<WashOrder> orders) {
        List<Long> deviceIds = orders.stream().map(WashOrder::getDeviceId).filter(id -> id != null).distinct().toList();
        if (deviceIds.isEmpty()) {
            return Map.of();
        }
        return deviceMapper.selectBatchIds(deviceIds).stream()
            .collect(Collectors.toMap(Device::getId, Function.identity(), (left, right) -> left));
    }

    private MiniAdminOrderItem toOrderItem(WashOrder order, Map<Long, Store> storeMap, Map<Long, Device> deviceMap) {
        Device device = deviceMap.get(order.getDeviceId());
        return new MiniAdminOrderItem(
            order.getId(),
            order.getOrderNo(),
            order.getUserId(),
            order.getStoreId(),
            resolveStoreName(storeMap, order.getStoreId()),
            order.getDeviceId(),
            device != null ? device.getDeviceName() : "",
            order.getPayMode(),
            order.getPaymentStatus(),
            order.getOrderStatus(),
            normalizeAmount(order.getFinalAmount()),
            order.getCreatedAt(),
            order.getStartTime(),
            order.getEndTime()
        );
    }

    private DeviceSimpleItem toDeviceItem(Device device, Map<Long, Store> storeMap) {
        Store store = storeMap.get(device.getStoreId());
        return new DeviceSimpleItem(
            device.getId(),
            device.getDeviceCode(),
            device.getDeviceName(),
            device.getStoreId(),
            store != null ? store.getStoreName() : "",
            device.getDeviceType(),
            device.getDeviceRole(),
            device.getDeviceStatus(),
            device.getDeviceStatus(),
            device.getProtocolType(),
            device.getFirmwareVersion(),
            device.getRemark(),
            device.getCreatedAt(),
            device.getUpdatedAt()
        );
    }

    private MiniAdminStoreOption resolveActiveStore(StoreScope scope) {
        if (scope.singleStoreId() == null) {
            return null;
        }
        Store store = storeMapper.selectById(scope.singleStoreId());
        return store != null ? new MiniAdminStoreOption(store.getId(), store.getFranchiseeId(), store.getStoreName()) : null;
    }

    private boolean isCountedWashOrder(WashOrder order) {
        if (order == null) {
            return false;
        }
        String status = normalizeStatus(order.getOrderStatus());
        return !"initial".equals(status) && !"cancelled".equals(status) && !"canceled".equals(status);
    }

    private String resolveStoreName(Map<Long, Store> storeMap, Long storeId) {
        if (storeId == null) {
            return "未知门店";
        }
        Store store = storeMap.get(storeId);
        return store != null && StringUtils.hasText(store.getStoreName()) ? store.getStoreName() : "门店" + storeId;
    }

    private String formatWalletActivityTitle(String bizType) {
        return switch (normalizeStatus(bizType)) {
            case "recharge" -> "钱包充值";
            case "consume" -> "钱包消费";
            case "refund" -> "钱包退款";
            case "fine" -> "余额罚款";
            default -> "钱包流水";
        };
    }

    private boolean isAbnormal(String status) {
        return "fault".equals(status) || "offline".equals(status) || "disabled".equals(status) || "paused".equals(status);
    }

    private int deviceSeverity(String status) {
        return switch (normalizeStatus(status)) {
            case "fault" -> 0;
            case "offline" -> 1;
            case "disabled" -> 2;
            case "paused" -> 3;
            default -> 9;
        };
    }

    private String normalizeStatus(String value) {
        return StringUtils.hasText(value) ? value.trim().toLowerCase() : "";
    }

    private BigDecimal normalizeAmount(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private record StoreScope(boolean allStores, List<Long> storeIds, Long singleStoreId) {

        static StoreScope unlimited() {
            return new StoreScope(true, List.of(), null);
        }

        static StoreScope single(Long storeId) {
            return new StoreScope(false, List.of(storeId), storeId);
        }

        static StoreScope limited(List<Long> storeIds) {
            return new StoreScope(false, storeIds != null ? storeIds : List.of(), null);
        }

        boolean isEmpty() {
            return !allStores && (storeIds == null || storeIds.isEmpty());
        }

        boolean needsInScope() {
            return !allStores && singleStoreId == null && storeIds != null && !storeIds.isEmpty();
        }
    }
}
