package com.washer.backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.washer.backend.common.ApiResponse;
import com.washer.backend.entity.Device;
import com.washer.backend.entity.Store;
import com.washer.backend.entity.UserCard;
import com.washer.backend.entity.UserInfo;
import com.washer.backend.entity.UserStoreWallet;
import com.washer.backend.entity.WashOrder;
import com.washer.backend.entity.WalletTransaction;
import com.washer.backend.mapper.UserCardMapper;
import com.washer.backend.mapper.UserStoreWalletMapper;
import com.washer.backend.mapper.WashOrderMapper;
import com.washer.backend.mapper.WalletTransactionMapper;
import com.washer.backend.service.DeviceService;
import com.washer.backend.service.StoreService;
import com.washer.backend.service.UserInfoService;
import com.washer.backend.service.WashPricingService;
import com.washer.backend.service.WashQueueService;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
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
@RequestMapping("/api/stores")
public class StoreController {

    private static final String MONTHLY_CARD_TYPE = "monthly";

    private final StoreService storeService;
    private final DeviceService deviceService;
    private final WashOrderMapper washOrderMapper;
    private final UserCardMapper userCardMapper;
    private final UserStoreWalletMapper userStoreWalletMapper;
    private final WalletTransactionMapper walletTransactionMapper;
    private final WashPricingService washPricingService;
    private final UserInfoService userInfoService;
    private final WashQueueService washQueueService;

    public StoreController(
        StoreService storeService,
        DeviceService deviceService,
        WashOrderMapper washOrderMapper,
        UserCardMapper userCardMapper,
        UserStoreWalletMapper userStoreWalletMapper,
        WalletTransactionMapper walletTransactionMapper,
        WashPricingService washPricingService,
        UserInfoService userInfoService,
        WashQueueService washQueueService
    ) {
        this.storeService = storeService;
        this.deviceService = deviceService;
        this.washOrderMapper = washOrderMapper;
        this.userCardMapper = userCardMapper;
        this.userStoreWalletMapper = userStoreWalletMapper;
        this.walletTransactionMapper = walletTransactionMapper;
        this.washPricingService = washPricingService;
        this.userInfoService = userInfoService;
        this.washQueueService = washQueueService;
    }

    @GetMapping
    public ApiResponse<Page<Store>> page(
        @RequestParam(defaultValue = "1") long page,
        @RequestParam(defaultValue = "10") long size,
        @RequestParam(required = false) String keyword
    ) {
        LambdaQueryWrapper<Store> wrapper = new LambdaQueryWrapper<Store>()
            .orderByDesc(Store::getId);

        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w
                .like(Store::getStoreName, keyword)
                .or()
                .like(Store::getStoreCode, keyword)
                .or()
                .like(Store::getAddress, keyword));
        }

        return ApiResponse.success(storeService.page(new Page<>(page, size), wrapper));
    }

    @GetMapping("/miniapp-list")
    public ApiResponse<Page<Map<String, Object>>> miniappList(
        @RequestParam(defaultValue = "1") long page,
        @RequestParam(defaultValue = "10") long size,
        @RequestParam(required = false) Long userId,
        @RequestParam(required = false) Double userLat,
        @RequestParam(required = false) Double userLng
    ) {
        Page<Store> storePage = storeService.page(new Page<>(page, size),
            new LambdaQueryWrapper<Store>().orderByDesc(Store::getId));
        List<Store> records = storePage.getRecords();
        if (records.isEmpty()) {
            return ApiResponse.success(new Page<>(page, size, 0));
        }

        List<Long> storeIds = records.stream().map(Store::getId).toList();
        List<Device> devices = getDevicesByStoreIds(storeIds);
        Map<Long, List<Device>> deviceMap = devices.stream()
            .collect(Collectors.groupingBy(Device::getStoreId));

        List<WashOrder> runningOrders = getRunningOrdersByStoreIds(storeIds);
        Map<Long, List<WashOrder>> runningMap = runningOrders.stream()
            .collect(Collectors.groupingBy(WashOrder::getStoreId));

        Map<Long, UserStoreWallet> walletMap = new HashMap<>();
        if (userId != null) {
            List<UserStoreWallet> wallets = userStoreWalletMapper.selectList(
                new LambdaQueryWrapper<UserStoreWallet>()
                    .eq(UserStoreWallet::getUserId, userId)
                    .in(UserStoreWallet::getStoreId, storeIds)
            );
            walletMap = wallets.stream()
                .collect(Collectors.toMap(UserStoreWallet::getStoreId, wallet -> wallet, (a, b) -> a));
        }
        Map<Long, CardAvailability> cardMap = buildAvailableCardMap(userId, storeIds);
        Set<Long> vipMonthlyStoreIds = buildActiveMonthlyStoreIds(userId, storeIds);
        boolean isRechargeMember = isRechargeMember(userId);
        boolean isMemberDay = isRechargeMember && washPricingService.isMemberDay(LocalDate.now(), java.time.LocalTime.now());

        List<Map<String, Object>> items = new ArrayList<>();
        for (Store store : records) {
            List<Device> storeDevices = deviceMap.getOrDefault(store.getId(), List.of());
            List<WashOrder> storeRunning = runningMap.getOrDefault(store.getId(), List.of());
            List<Map<String, Object>> bayStatusList = buildBayStatusList(storeDevices, storeRunning);

            int totalBays = bayStatusList.size();
            int usingBays = (int) bayStatusList.stream().filter(bay -> "using".equals(bay.get("status"))).count();
            List<String> usageSummary = buildUsageSummary(bayStatusList, 2);

            UserStoreWallet wallet = walletMap.get(store.getId());
            CardAvailability availableCard = cardMap.get(store.getId());
            Map<String, Object> item = new HashMap<>();
            item.put("id", store.getId());
            item.put("name", store.getStoreName());
            item.put("coverImage", resolveStoreCover(store));
            item.put("address", resolveStoreAddress(store));
            item.put("phone", store.getContactPhone());
            item.put("latitude", store.getLatitude());
            item.put("longitude", store.getLongitude());
            item.put("distanceKm", resolveDistanceKm(store, userLat, userLng));
            item.put("featureTags", parseFeatureTags(store.getFeatureTags()));
            boolean hasVipMonthlyCard = vipMonthlyStoreIds.contains(store.getId());
            boolean memberDayDiscountApplied = !hasVipMonthlyCard && isMemberDay;
            item.put("pricingRuleText", resolvePricingRuleText(store, hasVipMonthlyCard, memberDayDiscountApplied));
            item.put("normalPricingRuleText", resolvePricingRuleText(store, false, false));
            item.put("vipPricingRuleText", resolvePricingRuleText(store, true, false));
            item.put("memberDayPricingRuleText", resolvePricingRuleText(store, false, true));
            item.put("hasVipMonthlyCard", hasVipMonthlyCard);
            item.put("isMember", isRechargeMember);
            item.put("isMemberDay", isMemberDay);
            item.put("memberDayDiscountApplied", memberDayDiscountApplied);
            item.put("totalBays", totalBays);
            item.put("usingBays", usingBays);
            item.put("idleBays", Math.max(totalBays - usingBays - countUnavailableBays(bayStatusList), 0));
            appendQueueFields(item, userId, store.getId());
            item.put("usageSummary", usageSummary);
            item.put("bayStatusList", bayStatusList);
            item.put("principalBalance", resolveWalletPrincipal(wallet));
            item.put("giftBalance", resolveWalletGift(wallet));
            appendCardFields(item, availableCard);
            items.add(item);
        }

        Page<Map<String, Object>> result = new Page<>(storePage.getCurrent(), storePage.getSize(), storePage.getTotal());
        result.setRecords(items);
        return ApiResponse.success(result);
    }

    @GetMapping("/{id}/miniapp-detail")
    public ApiResponse<Map<String, Object>> miniappDetail(
        @PathVariable Long id,
        @RequestParam(required = false) Long userId,
        @RequestParam(required = false) Double userLat,
        @RequestParam(required = false) Double userLng
    ) {
        Store store = storeService.getById(id);
        if (store == null) {
            throw new IllegalArgumentException("store not found");
        }

        List<Device> devices = getDevicesByStoreIds(List.of(id));
        List<WashOrder> runningOrders = getRunningOrdersByStoreIds(List.of(id));
        List<Map<String, Object>> bayList = buildBayStatusList(devices, runningOrders);

        UserStoreWallet wallet = null;
        CardAvailability availableCard = null;
        boolean hasVipMonthlyCard = false;
        boolean isRechargeMember = isRechargeMember(userId);
        boolean isMemberDay = isRechargeMember && washPricingService.isMemberDay(LocalDate.now(), java.time.LocalTime.now());
        if (userId != null) {
            wallet = userStoreWalletMapper.selectOne(
                new LambdaQueryWrapper<UserStoreWallet>()
                    .eq(UserStoreWallet::getUserId, userId)
                    .eq(UserStoreWallet::getStoreId, id)
                    .last("limit 1")
            );
            availableCard = findCardAvailability(userId, id);
            hasVipMonthlyCard = hasActiveMonthlyCard(userId, id);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("id", store.getId());
        result.put("name", store.getStoreName());
        result.put("coverImage", resolveStoreCover(store));
        result.put("address", resolveStoreAddress(store));
        result.put("phone", store.getContactPhone());
        result.put("latitude", store.getLatitude());
        result.put("longitude", store.getLongitude());
        result.put("distanceKm", resolveDistanceKm(store, userLat, userLng));
        result.put("featureTags", parseFeatureTags(store.getFeatureTags()));
        boolean memberDayDiscountApplied = !hasVipMonthlyCard && isMemberDay;
        result.put("pricingRuleText", resolvePricingRuleText(store, hasVipMonthlyCard, memberDayDiscountApplied));
        result.put("normalPricingRuleText", resolvePricingRuleText(store, false, false));
        result.put("vipPricingRuleText", resolvePricingRuleText(store, true, false));
        result.put("memberDayPricingRuleText", resolvePricingRuleText(store, false, true));
        result.put("hasVipMonthlyCard", hasVipMonthlyCard);
        result.put("isMember", isRechargeMember);
        result.put("isMemberDay", isMemberDay);
        result.put("memberDayDiscountApplied", memberDayDiscountApplied);
        result.put("totalBays", bayList.size());
        result.put("usingBays", (int) bayList.stream().filter(bay -> "using".equals(bay.get("status"))).count());
        result.put(
            "idleBays",
            Math.max(
                bayList.size()
                    - (int) bayList.stream().filter(bay -> "using".equals(bay.get("status"))).count()
                    - countUnavailableBays(bayList),
                0
            )
        );
        appendQueueFields(result, userId, id);
        result.put("bays", bayList);
        result.put("bayStatusList", bayList);
        result.put("principalBalance", resolveWalletPrincipal(wallet));
        result.put("giftBalance", resolveWalletGift(wallet));
        appendCardFields(result, availableCard);
        return ApiResponse.success(result);
    }

    @GetMapping("/bay-status")
    public ApiResponse<List<Map<String, Object>>> bayStatus(
        @RequestParam(required = false) String storeId
    ) {
        Long parsedStoreId = parseLong(storeId);
        List<Store> stores = storeService.list(
            new LambdaQueryWrapper<Store>()
                .eq(parsedStoreId != null, Store::getId, parsedStoreId)
                .eq(parsedStoreId == null && StringUtils.hasText(storeId), Store::getStoreName, storeId)
                .orderByDesc(Store::getId)
        );
        if (stores.isEmpty()) {
            return ApiResponse.success(List.of());
        }

        List<Long> storeIds = stores.stream().map(Store::getId).toList();
        Map<Long, List<Device>> deviceMap = getDevicesByStoreIds(storeIds).stream()
            .collect(Collectors.groupingBy(Device::getStoreId));
        Map<Long, List<WashOrder>> runningMap = getRunningOrdersByStoreIds(storeIds).stream()
            .collect(Collectors.groupingBy(WashOrder::getStoreId));

        List<Map<String, Object>> result = stores.stream()
            .map(store -> buildStoreBayStatus(
                store,
                deviceMap.getOrDefault(store.getId(), List.of()),
                runningMap.getOrDefault(store.getId(), List.of())
            ))
            .toList();

        return ApiResponse.success(result);
    }

    @GetMapping("/{id}")
    public ApiResponse<Store> getById(@PathVariable Long id) {
        Store store = storeService.getById(id);
        if (store == null) {
            throw new IllegalArgumentException("store not found");
        }
        return ApiResponse.success(store);
    }

    @PostMapping
    public ApiResponse<Store> create(@RequestBody Store store) {
        if (!StringUtils.hasText(store.getStoreCode())) {
            store.setStoreCode("S" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
        }
        if (!StringUtils.hasText(store.getStoreName())) {
            throw new IllegalArgumentException("storeName不能为空");
        }
        if (store.getStoreStatus() == null) {
            store.setStoreStatus(1);
        }
        storeService.save(store);
        return ApiResponse.success("创建成功", store);
    }

    @PutMapping("/{id}")
    public ApiResponse<Store> update(@PathVariable Long id, @RequestBody Store store) {
        store.setId(id);
        boolean updated = storeService.updateById(store);
        if (!updated) {
            throw new IllegalArgumentException("门店不存在或更新失败");
        }
        return ApiResponse.success("更新成功", storeService.getById(id));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Boolean> delete(@PathVariable Long id) {
        boolean removed = storeService.removeById(id);
        if (!removed) {
            throw new IllegalArgumentException("门店不存在或删除失败");
        }
        return ApiResponse.success("删除成功", true);
    }

    private String resolveStoreCover(Store store) {
        if (store != null && StringUtils.hasText(store.getRemark()) && store.getRemark().startsWith("http")) {
            return store.getRemark();
        }
        return "/assets/images/washing.png";
    }

    private String resolveStoreAddress(Store store) {
        if (store == null) {
            return "";
        }
        if (StringUtils.hasText(store.getAddress())) {
            return store.getAddress();
        }
        String combined = String.join("", List.of(
            valueOrEmpty(store.getProvince()),
            valueOrEmpty(store.getCity()),
            valueOrEmpty(store.getDistrict())
        ));
        return StringUtils.hasText(combined) ? combined : "地址待补充";
    }

    private String resolvePricingRuleText(Store store, boolean vipPricing, boolean memberDayDiscount) {
        return washPricingService.resolvePricingRuleText(store != null ? store.getId() : null, vipPricing, memberDayDiscount);
    }

    private boolean isRechargeMember(Long userId) {
        if (userId == null) {
            return false;
        }
        UserInfo userInfo = userInfoService.getById(userId);
        if (userInfo != null && Integer.valueOf(1).equals(userInfo.getIsMember())) {
            return true;
        }
        if (!hasRechargeHistory(userId)) {
            return false;
        }
        if (userInfo != null) {
            userInfo.setIsMember(1);
            if (!StringUtils.hasText(userInfo.getMemberLevel())) {
                userInfo.setMemberLevel("normal");
            }
            if (userInfo.getMemberSinceTime() == null) {
                userInfo.setMemberSinceTime(LocalDateTime.now());
            }
            userInfoService.updateById(userInfo);
        }
        return true;
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

    private Double resolveDistanceKm(Store store, Double userLat, Double userLng) {
        if (store == null || store.getLatitude() == null || store.getLongitude() == null) {
            return null;
        }
        if (userLat == null || userLng == null) {
            return null;
        }
        double distance = haversine(userLat, userLng, store.getLatitude().doubleValue(), store.getLongitude().doubleValue());
        return Math.round(distance * 100.0) / 100.0;
    }

    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
            + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
            * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return 6371.0 * c;
    }

    private List<Device> getDevicesByStoreIds(List<Long> storeIds) {
        if (storeIds == null || storeIds.isEmpty()) {
            return List.of();
        }
        return deviceService.list(
            new LambdaQueryWrapper<Device>()
                .in(Device::getStoreId, storeIds)
                .orderByAsc(Device::getStoreId)
                .orderByAsc(Device::getId)
        );
    }

    private List<WashOrder> getRunningOrdersByStoreIds(List<Long> storeIds) {
        if (storeIds == null || storeIds.isEmpty()) {
            return List.of();
        }
        return washOrderMapper.selectList(
            new LambdaQueryWrapper<WashOrder>()
                .in(WashOrder::getStoreId, storeIds)
                .eq(WashOrder::getOrderStatus, "running")
        );
    }

    private Map<String, Object> buildStoreBayStatus(Store store, List<Device> devices, List<WashOrder> runningOrders) {
        List<Map<String, Object>> bayStatusList = buildBayStatusList(devices, runningOrders);
        int usingBays = (int) bayStatusList.stream()
            .filter(bay -> "using".equals(bay.get("status")))
            .count();

        Map<String, Object> result = new HashMap<>();
        result.put("storeId", store.getId());
        result.put("storeName", store.getStoreName());
        result.put("totalBays", bayStatusList.size());
        result.put("usingBays", usingBays);
        result.put("bayStatusList", bayStatusList);
        return result;
    }

    private List<Map<String, Object>> buildBayStatusList(List<Device> devices, List<WashOrder> runningOrders) {
        if (devices == null || devices.isEmpty()) {
            return List.of();
        }

        return devices.stream()
            .sorted(Comparator.comparing(Device::getId))
            .map(device -> {
                WashOrder running = findRunningOrder(device, runningOrders);
                boolean isUsing = running != null;
                long usingMinutes = isUsing ? resolveUsedMinutes(running.getStartTime()) : 0;
                Long bayId = resolveBayId(device);
                String bayName = resolveDeviceName(device);
                String deviceStatus = normalizeDeviceStatus(device.getDeviceStatus());
                String status = resolveBayStatus(isUsing, deviceStatus);
                boolean canStart = !isUsing && isDeviceStartable(deviceStatus);
                String unavailableReason = canStart ? "" : resolveUnavailableReason(status);

                Map<String, Object> item = new HashMap<>();
                item.put("bayId", bayId);
                item.put("deviceId", device.getId());
                item.put("deviceCode", device.getDeviceCode());
                item.put("deviceStatus", deviceStatus);
                item.put("bayName", bayName);
                item.put("status", status);
                item.put("canStart", canStart);
                item.put("unavailableReason", unavailableReason);
                item.put("usingMinutes", usingMinutes);

                // Keep the old detail-page field names so existing miniapp code does not break.
                item.put("id", bayId);
                item.put("name", bayName);
                item.put("usedMinutes", usingMinutes);
                return item;
            })
            .toList();
    }

    private String normalizeDeviceStatus(String value) {
        return StringUtils.hasText(value) ? value.trim().toLowerCase() : "";
    }

    private String resolveBayStatus(boolean isUsing, String deviceStatus) {
        if (isUsing || "running".equals(deviceStatus)) {
            return "using";
        }
        if ("offline".equals(deviceStatus)
            || "fault".equals(deviceStatus)
            || "disabled".equals(deviceStatus)
            || "paused".equals(deviceStatus)) {
            return deviceStatus;
        }
        return "idle";
    }

    private boolean isDeviceStartable(String deviceStatus) {
        if (!StringUtils.hasText(deviceStatus)) {
            return true;
        }
        return "idle".equals(deviceStatus) || "online".equals(deviceStatus);
    }

    private String resolveUnavailableReason(String status) {
        return switch (status) {
            case "using" -> "工位洗车中";
            case "offline" -> "设备已离线，暂时无法开始洗车";
            case "fault" -> "设备故障，请选择其他工位";
            case "disabled" -> "设备已停用，请选择其他工位";
            case "paused" -> "设备暂停服务，请选择其他工位";
            default -> "当前工位不可用";
        };
    }

    private int countUnavailableBays(List<Map<String, Object>> bayStatusList) {
        if (bayStatusList == null || bayStatusList.isEmpty()) {
            return 0;
        }
        return (int) bayStatusList.stream()
            .filter(bay -> {
                String status = String.valueOf(bay.get("status"));
                return "offline".equals(status)
                    || "fault".equals(status)
                    || "disabled".equals(status)
                    || "paused".equals(status)
                    || "unavailable".equals(status);
            })
            .count();
    }

    private void appendQueueFields(Map<String, Object> target, Long userId, Long storeId) {
        Map<String, Object> queueInfo = washQueueService.getQueueStatus(userId, storeId);
        target.put("queueInfo", queueInfo);
        target.put("queueActive", queueInfo.get("active"));
        target.put("queueAheadCount", queueInfo.get("aheadCount"));
        target.put("queuePosition", queueInfo.get("position"));
        target.put("queueNo", queueInfo.get("queueNo"));
    }

    private WashOrder findRunningOrder(Device device, List<WashOrder> runningOrders) {
        if (device == null || runningOrders == null || runningOrders.isEmpty()) {
            return null;
        }
        return runningOrders.stream()
            .filter(order -> matchesBay(device, order))
            .sorted(Comparator.comparing(WashOrder::getStartTime, Comparator.nullsFirst(Comparator.naturalOrder())).reversed())
            .findFirst()
            .orElse(null);
    }

    private boolean matchesBay(Device device, WashOrder order) {
        if (device == null || order == null) {
            return false;
        }
        Long bayId = resolveBayId(device);
        boolean sameDevice = order.getDeviceId() != null && order.getDeviceId().equals(device.getId());
        boolean sameBay = order.getBayId() != null && order.getBayId().equals(bayId);
        return sameDevice || sameBay;
    }

    private Long resolveBayId(Device device) {
        if (device == null) {
            return null;
        }
        return device.getBayId() != null ? device.getBayId() : device.getId();
    }

    private List<String> buildUsageSummary(List<Map<String, Object>> bayStatusList, int limit) {
        if (bayStatusList == null || bayStatusList.isEmpty()) {
            return List.of();
        }

        List<Map<String, Object>> usingBays = bayStatusList.stream()
            .filter(bay -> "using".equals(bay.get("status")))
            .sorted((left, right) -> Long.compare(toLong(right.get("usingMinutes")), toLong(left.get("usingMinutes"))))
            .toList();
        if (usingBays.isEmpty()) {
            return List.of();
        }

        List<String> summary = new ArrayList<>();
        for (int i = 0; i < usingBays.size() && i < limit; i++) {
            Map<String, Object> bay = usingBays.get(i);
            summary.add(bay.get("bayName") + "已使用" + bay.get("usingMinutes") + "分钟");
        }
        if (usingBays.size() > limit) {
            summary.add("等" + (usingBays.size() - limit) + "个工位使用中");
        }
        return summary;
    }

    private long resolveUsedMinutes(LocalDateTime startTime) {
        if (startTime == null) {
            return 0;
        }
        return Math.max(0, Duration.between(startTime, LocalDateTime.now()).toMinutes());
    }

    private long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return 0;
    }

    private Long parseLong(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String resolveDeviceName(Device device) {
        if (device == null) {
            return "工位";
        }
        if (StringUtils.hasText(device.getDeviceName())) {
            return device.getDeviceName();
        }
        if (StringUtils.hasText(device.getDeviceCode())) {
            return device.getDeviceCode();
        }
        return "工位" + device.getId();
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }

    private List<String> parseFeatureTags(String value) {
        if (!StringUtils.hasText(value)) {
            return List.of();
        }
        List<String> tags = new ArrayList<>();
        for (String part : value.split("[,，;；\\r\\n]+")) {
            String tag = part == null ? "" : part.trim();
            if (StringUtils.hasText(tag)) {
                tags.add(tag);
            }
        }
        return tags;
    }

    private String resolveWalletPrincipal(UserStoreWallet wallet) {
        if (wallet == null || wallet.getAvailablePrincipalBalance() == null) {
            return "0.00";
        }
        return wallet.getAvailablePrincipalBalance().toPlainString();
    }

    private String resolveWalletGift(UserStoreWallet wallet) {
        if (wallet == null || wallet.getAvailableGiftBalance() == null) {
            return "0.00";
        }
        return wallet.getAvailableGiftBalance().toPlainString();
    }

    private Set<Long> buildActiveMonthlyStoreIds(Long userId, List<Long> storeIds) {
        if (userId == null || storeIds == null || storeIds.isEmpty()) {
            return Set.of();
        }
        LocalDateTime now = LocalDateTime.now();
        return userCardMapper.selectList(
            new LambdaQueryWrapper<UserCard>()
                .eq(UserCard::getUserId, userId)
                .in(UserCard::getStoreId, storeIds)
                .eq(UserCard::getCardType, MONTHLY_CARD_TYPE)
                .eq(UserCard::getStatus, "active")
                .and(wrapper -> wrapper.isNull(UserCard::getEffectiveTime).or().le(UserCard::getEffectiveTime, now))
                .and(wrapper -> wrapper.isNull(UserCard::getExpireTime).or().gt(UserCard::getExpireTime, now))
        ).stream()
            .map(UserCard::getStoreId)
            .filter(id -> id != null)
            .collect(Collectors.toSet());
    }

    private boolean hasActiveMonthlyCard(Long userId, Long storeId) {
        if (userId == null || storeId == null) {
            return false;
        }
        return buildActiveMonthlyStoreIds(userId, List.of(storeId)).contains(storeId);
    }

    private Map<Long, CardAvailability> buildAvailableCardMap(Long userId, List<Long> storeIds) {
        if (userId == null || storeIds == null || storeIds.isEmpty()) {
            return Map.of();
        }

        LocalDateTime now = LocalDateTime.now();
        List<UserCard> cards = userCardMapper.selectList(
            new LambdaQueryWrapper<UserCard>()
                .eq(UserCard::getUserId, userId)
                .in(UserCard::getStoreId, storeIds)
                .eq(UserCard::getStatus, "active")
                .gt(UserCard::getRemainingTimes, 0)
                .and(wrapper -> wrapper.isNull(UserCard::getEffectiveTime).or().le(UserCard::getEffectiveTime, now))
                .and(wrapper -> wrapper.isNull(UserCard::getExpireTime).or().gt(UserCard::getExpireTime, now))
                .orderByAsc(UserCard::getId)
        );

        Map<Long, List<UserCard>> groupedCards = cards.stream()
            .collect(Collectors.groupingBy(UserCard::getStoreId));
        Map<Long, CardAvailability> result = new HashMap<>();
        for (Map.Entry<Long, List<UserCard>> entry : groupedCards.entrySet()) {
            List<UserCard> storeCards = entry.getValue();
            if (storeCards == null || storeCards.isEmpty()) {
                continue;
            }
            int remainingTimes = storeCards.stream()
                .map(UserCard::getRemainingTimes)
                .filter(times -> times != null && times > 0)
                .reduce(0, Integer::sum);
            result.put(entry.getKey(), new CardAvailability(storeCards.get(0), remainingTimes));
        }
        return result;
    }

    private CardAvailability findCardAvailability(Long userId, Long storeId) {
        if (userId == null || storeId == null) {
            return null;
        }

        LocalDateTime now = LocalDateTime.now();
        List<UserCard> cards = userCardMapper.selectList(
            new LambdaQueryWrapper<UserCard>()
                .eq(UserCard::getUserId, userId)
                .eq(UserCard::getStoreId, storeId)
                .eq(UserCard::getStatus, "active")
                .gt(UserCard::getRemainingTimes, 0)
                .and(wrapper -> wrapper.isNull(UserCard::getEffectiveTime).or().le(UserCard::getEffectiveTime, now))
                .and(wrapper -> wrapper.isNull(UserCard::getExpireTime).or().gt(UserCard::getExpireTime, now))
                .orderByAsc(UserCard::getId)
        );
        if (cards.isEmpty()) {
            return null;
        }
        int remainingTimes = cards.stream()
            .map(UserCard::getRemainingTimes)
            .filter(times -> times != null && times > 0)
            .reduce(0, Integer::sum);
        return new CardAvailability(cards.get(0), remainingTimes);
    }

    private void appendCardFields(Map<String, Object> target, CardAvailability cardAvailability) {
        UserCard availableCard = cardAvailability != null ? cardAvailability.firstCard() : null;
        target.put("hasAvailableCard", availableCard != null);
        target.put("availableCardId", availableCard != null ? availableCard.getId() : null);
        target.put("availableCardNo", availableCard != null ? availableCard.getCardNo() : "");
        target.put(
            "availableCardRemainingTimes",
            cardAvailability != null ? cardAvailability.remainingTimes() : 0
        );
    }

    private record CardAvailability(UserCard firstCard, int remainingTimes) {
    }
}
