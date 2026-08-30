package com.washer.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.washer.backend.dto.admin.AdminUserAssetOverview;
import com.washer.backend.dto.admin.AdminUserCardAdjustResult;
import com.washer.backend.dto.admin.AdminUserCardAsset;
import com.washer.backend.dto.admin.AdminUserCardDetail;
import com.washer.backend.dto.admin.AdminUserCardManualAddRequest;
import com.washer.backend.dto.admin.AdminUserCardManualReduceRequest;
import com.washer.backend.dto.admin.AdminUserCardPageItem;
import com.washer.backend.dto.admin.AdminUserCardUsageItem;
import com.washer.backend.dto.admin.AdminUserCreateRequest;
import com.washer.backend.dto.admin.AdminUserListItem;
import com.washer.backend.dto.admin.AdminUserRecentOrder;
import com.washer.backend.dto.admin.AdminUserUpdateRequest;
import com.washer.backend.dto.admin.AdminUserWalletAsset;
import com.washer.backend.dto.admin.AdminUserWalletTransactionItem;
import com.washer.backend.entity.CardPurchaseOrder;
import com.washer.backend.entity.CardUsageRecord;
import com.washer.backend.entity.Device;
import com.washer.backend.entity.Store;
import com.washer.backend.entity.UserCard;
import com.washer.backend.entity.UserDailyDiscountRecord;
import com.washer.backend.entity.UserInfo;
import com.washer.backend.entity.UserStoreWallet;
import com.washer.backend.entity.WalletTransaction;
import com.washer.backend.entity.WashOrder;
import com.washer.backend.mapper.CardPurchaseOrderMapper;
import com.washer.backend.mapper.CardUsageRecordMapper;
import com.washer.backend.mapper.UserCardMapper;
import com.washer.backend.mapper.UserDailyDiscountRecordMapper;
import com.washer.backend.mapper.UserInfoMapper;
import com.washer.backend.mapper.UserStoreWalletMapper;
import com.washer.backend.mapper.WalletTransactionMapper;
import com.washer.backend.mapper.WashOrderMapper;
import com.washer.backend.service.DeviceService;
import com.washer.backend.service.StoreService;
import com.washer.backend.service.UserInfoService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo> implements UserInfoService {

    private static final String DISCOUNT_TYPE_FIRST_PERIOD = "first_period_discount";
    private static final String CARD_STATUS_ACTIVE = "active";
    private static final String CARD_STATUS_CANCELLED = "cancelled";
    private static final long MANUAL_CARD_PRODUCT_ID = 0L;
    private static final int DEFAULT_MANUAL_CARD_VALID_DAYS = 180;
    private static final DateTimeFormatter ADMIN_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final StoreService storeService;
    private final DeviceService deviceService;
    private final UserStoreWalletMapper userStoreWalletMapper;
    private final UserCardMapper userCardMapper;
    private final CardPurchaseOrderMapper cardPurchaseOrderMapper;
    private final WalletTransactionMapper walletTransactionMapper;
    private final CardUsageRecordMapper cardUsageRecordMapper;
    private final UserDailyDiscountRecordMapper userDailyDiscountRecordMapper;
    private final WashOrderMapper washOrderMapper;

    public UserInfoServiceImpl(
        StoreService storeService,
        DeviceService deviceService,
        UserStoreWalletMapper userStoreWalletMapper,
        UserCardMapper userCardMapper,
        CardPurchaseOrderMapper cardPurchaseOrderMapper,
        WalletTransactionMapper walletTransactionMapper,
        CardUsageRecordMapper cardUsageRecordMapper,
        UserDailyDiscountRecordMapper userDailyDiscountRecordMapper,
        WashOrderMapper washOrderMapper
    ) {
        this.storeService = storeService;
        this.deviceService = deviceService;
        this.userStoreWalletMapper = userStoreWalletMapper;
        this.userCardMapper = userCardMapper;
        this.cardPurchaseOrderMapper = cardPurchaseOrderMapper;
        this.walletTransactionMapper = walletTransactionMapper;
        this.cardUsageRecordMapper = cardUsageRecordMapper;
        this.userDailyDiscountRecordMapper = userDailyDiscountRecordMapper;
        this.washOrderMapper = washOrderMapper;
    }

    @Override
    public Page<AdminUserListItem> pageAdminUsers(long page, long size, String keyword) {
        LambdaQueryWrapper<UserInfo> wrapper = new LambdaQueryWrapper<UserInfo>()
            .orderByDesc(UserInfo::getId);

        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w
                .like(UserInfo::getNickname, keyword)
                .or()
                .like(UserInfo::getMobile, keyword)
                .or()
                .like(UserInfo::getUserNo, keyword)
                .or()
                .like(UserInfo::getRealName, keyword));
        }

        Page<UserInfo> userPage = this.page(new Page<>(page, size), wrapper);
        Page<AdminUserListItem> result = new Page<>(userPage.getCurrent(), userPage.getSize(), userPage.getTotal());
        result.setRecords(userPage.getRecords().stream()
            .map(user -> {
                normalizeExpiredMembership(user);
                return new AdminUserListItem(
                    user.getId(),
                    user.getUserNo(),
                    user.getNickname(),
                    user.getRealName(),
                    user.getMobile(),
                    user.getUserStatus(),
                    user.getRegisterSource(),
                    user.getIsMember(),
                    user.getMemberLevel(),
                    user.getPoints(),
                    user.getMemberExpireTime(),
                    user.getLastConsumeTime(),
                    user.getCreatedAt()
                );
            })
            .toList());
        return result;
    }

    @Override
    public AdminUserAssetOverview getAdminUserAssetOverview(Long id) {
        UserInfo user = getRequiredUser(id);
        normalizeExpiredMembership(user);
        List<UserStoreWallet> wallets = userStoreWalletMapper.selectList(
            new LambdaQueryWrapper<UserStoreWallet>()
                .eq(UserStoreWallet::getUserId, id)
                .orderByDesc(UserStoreWallet::getUpdatedAt)
                .orderByDesc(UserStoreWallet::getId)
        );
        List<UserCard> cards = userCardMapper.selectList(
            new LambdaQueryWrapper<UserCard>()
                .eq(UserCard::getUserId, id)
                .orderByDesc(UserCard::getUpdatedAt)
                .orderByDesc(UserCard::getId)
        );
        List<WashOrder> recentOrders = washOrderMapper.selectList(
            new LambdaQueryWrapper<WashOrder>()
                .eq(WashOrder::getUserId, id)
                .orderByDesc(WashOrder::getId)
                .last("limit 5")
        );
        List<WalletTransaction> recentWalletTransactions = walletTransactionMapper.selectList(
            new LambdaQueryWrapper<WalletTransaction>()
                .eq(WalletTransaction::getUserId, id)
                .orderByDesc(WalletTransaction::getId)
                .last("limit 5")
        );
        List<CardUsageRecord> recentCardUsages = cardUsageRecordMapper.selectList(
            new LambdaQueryWrapper<CardUsageRecord>()
                .eq(CardUsageRecord::getUserId, id)
                .orderByDesc(CardUsageRecord::getId)
                .last("limit 5")
        );
        UserDailyDiscountRecord todayDiscountRecord = userDailyDiscountRecordMapper.selectOne(
            new LambdaQueryWrapper<UserDailyDiscountRecord>()
                .eq(UserDailyDiscountRecord::getUserId, id)
                .eq(UserDailyDiscountRecord::getDiscountDate, LocalDate.now())
                .eq(UserDailyDiscountRecord::getDiscountType, DISCOUNT_TYPE_FIRST_PERIOD)
                .orderByDesc(UserDailyDiscountRecord::getId)
                .last("limit 1")
        );

        Map<Long, Store> storeMap = buildStoreMap(wallets, cards, recentOrders, recentWalletTransactions, recentCardUsages, todayDiscountRecord);
        Map<Long, Device> deviceMap = buildDeviceMap(recentOrders);

        return new AdminUserAssetOverview(
            user.getId(),
            user.getUserNo(),
            user.getNickname(),
            user.getRealName(),
            user.getMobile(),
            user.getUserStatus(),
            user.getRegisterSource(),
            user.getIsMember(),
            user.getMemberLevel(),
            user.getPoints(),
            user.getMemberSinceTime(),
            user.getMemberExpireTime(),
            user.getLastLoginTime(),
            user.getLastConsumeTime(),
            user.getRemark(),
            user.getCreatedAt(),
            todayDiscountRecord != null ? 1 : 0,
            todayDiscountRecord != null ? todayDiscountRecord.getDiscountDate() : null,
            todayDiscountRecord != null ? todayDiscountRecord.getStoreId() : null,
            todayDiscountRecord != null ? resolveStoreName(storeMap, todayDiscountRecord.getStoreId()) : "",
            todayDiscountRecord != null ? todayDiscountRecord.getOrderId() : null,
            todayDiscountRecord != null ? todayDiscountRecord.getOrderNo() : "",
            todayDiscountRecord != null ? normalizeAmount(todayDiscountRecord.getDiscountAmount()) : BigDecimal.ZERO,
            wallets.stream().map(wallet -> toWalletAsset(wallet, storeMap)).toList(),
            cards.stream().map(card -> toCardAsset(card, storeMap)).toList(),
            recentOrders.stream().map(order -> toRecentOrder(order, storeMap, deviceMap)).toList(),
            recentWalletTransactions.stream().map(tx -> toWalletTransactionItem(tx, storeMap)).toList(),
            recentCardUsages.stream().map(record -> toCardUsageItem(record, storeMap)).toList()
        );
    }

    @Override
    public UserInfo createAdminUser(AdminUserCreateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request is required");
        }
        UserInfo user = new UserInfo();
        if (StringUtils.hasText(request.getUserNo())) {
            user.setUserNo(request.getUserNo());
        } else {
            user.setUserNo("U" + UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        }
        if (StringUtils.hasText(request.getNickname())) {
            user.setNickname(request.getNickname());
        } else {
            user.setNickname("微信用户");
        }
        user.setRealName(request.getRealName());
        user.setMobile(request.getMobile());
        user.setRemark(request.getRemark());
        user.setUserStatus(request.getUserStatus() != null ? request.getUserStatus() : 1);
        user.setRegisterSource("admin");
        user.setIsMember(0);
        user.setMemberLevel("normal");
        this.save(user);
        return user;
    }

    @Override
    public UserInfo updateAdminUser(Long id, AdminUserUpdateRequest request) {
        if (id == null) {
            throw new IllegalArgumentException("userId is required");
        }
        if (request == null) {
            throw new IllegalArgumentException("request is required");
        }
        UserInfo existing = getRequiredUser(id);
        if (request.getNickname() != null) {
            existing.setNickname(request.getNickname());
        }
        if (request.getRealName() != null) {
            existing.setRealName(request.getRealName());
        }
        if (request.getMobile() != null) {
            existing.setMobile(request.getMobile());
        }
        if (request.getUserStatus() != null) {
            existing.setUserStatus(request.getUserStatus());
        }
        if (request.getRemark() != null) {
            existing.setRemark(request.getRemark());
        }
        this.updateById(existing);
        return this.getById(id);
    }

    @Override
    public Page<AdminUserCardPageItem> pageAdminUserCards(
        Long userId,
        long page,
        long size,
        Long storeId,
        String status,
        String cardNo
    ) {
        getRequiredUser(userId);
        LambdaQueryWrapper<UserCard> wrapper = new LambdaQueryWrapper<UserCard>()
            .eq(UserCard::getUserId, userId)
            .eq(storeId != null, UserCard::getStoreId, storeId)
            .eq(StringUtils.hasText(status), UserCard::getStatus, normalizeText(status))
            .like(StringUtils.hasText(cardNo), UserCard::getCardNo, normalizeText(cardNo))
            .orderByDesc(UserCard::getId);

        Page<UserCard> cardPage = userCardMapper.selectPage(new Page<>(page, size), wrapper);
        Map<Long, Store> storeMap = buildStoreMapFromCards(cardPage.getRecords());
        Page<AdminUserCardPageItem> result = new Page<>(cardPage.getCurrent(), cardPage.getSize(), cardPage.getTotal());
        result.setRecords(cardPage.getRecords().stream()
            .map(card -> toCardPageItem(card, storeMap))
            .toList());
        return result;
    }

    @Override
    public AdminUserCardDetail getAdminUserCardDetail(Long userId, Long cardId) {
        getRequiredUser(userId);
        UserCard card = getRequiredUserCard(userId, cardId);
        Map<Long, Store> storeMap = buildStoreMapFromCards(List.of(card));
        List<CardUsageRecord> usages = cardUsageRecordMapper.selectList(
            new LambdaQueryWrapper<CardUsageRecord>()
                .eq(CardUsageRecord::getUserCardId, card.getId())
                .orderByDesc(CardUsageRecord::getId)
        );
        Map<Long, Store> usageStoreMap = buildStoreMapFromUsages(usages);
        AdminUserCardDetail detail = new AdminUserCardDetail();
        detail.setCard(toCardPageItem(card, storeMap));
        detail.setUsageRecords(usages.stream()
            .map(record -> toCardUsageItem(record, usageStoreMap))
            .toList());
        return detail;
    }

    @Override
    @Transactional
    public AdminUserCardAdjustResult manualAddUserCards(Long userId, AdminUserCardManualAddRequest request) {
        getRequiredUser(userId);
        if (request == null) {
            throw new IllegalArgumentException("request is required");
        }
        Store store = requireStore(request.getStoreId());
        int count = normalizePositiveCount(request.getCount(), "count is required");
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime effectiveTime = parseAdminDateTime(request.getEffectiveTime(), now);
        LocalDateTime expireTime = parseAdminDateTime(request.getExpireTime(), effectiveTime.plusDays(DEFAULT_MANUAL_CARD_VALID_DAYS));

        if (!expireTime.isAfter(effectiveTime)) {
            throw new IllegalArgumentException("expireTime must be after effectiveTime");
        }

        CardPurchaseOrder purchaseOrder = createManualCardPurchaseOrder(userId, store.getId(), count, now, request.getRemark());
        AdminUserCardAdjustResult result = new AdminUserCardAdjustResult();
        List<Long> ids = new ArrayList<>(count);
        for (int i = 0; i < count; i += 1) {
            UserCard card = new UserCard();
            card.setUserId(userId);
            card.setStoreId(store.getId());
            card.setCardProductId(MANUAL_CARD_PRODUCT_ID);
            card.setCardType("manual");
            card.setSourceChannel("admin");
            card.setCardNo(buildManualCardNo());
            card.setTotalTimes(1);
            card.setUsedTimes(0);
            card.setRemainingTimes(1);
            card.setPurchaseTime(now);
            card.setEffectiveTime(effectiveTime);
            card.setExpireTime(expireTime);
            card.setStatus(CARD_STATUS_ACTIVE);
            card.setExternalOrderNo(purchaseOrder.getPurchaseOrderNo());
            card.setRemark(buildManualAddRemark(request.getRemark()));
            card.setCreatedAt(now);
            card.setUpdatedAt(now);
            userCardMapper.insert(card);
            ids.add(card.getId());
        }

        result.setAffectedCount(ids.size());
        result.setUserCardIds(ids);
        return result;
    }

    private CardPurchaseOrder createManualCardPurchaseOrder(Long userId, Long storeId, int count, LocalDateTime now, String remark) {
        CardPurchaseOrder order = new CardPurchaseOrder();
        order.setPurchaseOrderNo("CP" + UUID.randomUUID().toString().replace("-", "").substring(0, 18));
        order.setUserId(userId);
        order.setStoreId(storeId);
        order.setCardProductId(MANUAL_CARD_PRODUCT_ID);
        order.setCardType("manual");
        order.setSourceChannel("admin");
        order.setBuyCount(count);
        order.setPayAmount(BigDecimal.ZERO);
        order.setPayStatus("paid");
        order.setPurchaseTime(now);
        order.setRemark(buildManualAddRemark(remark));
        cardPurchaseOrderMapper.insert(order);
        return order;
    }

    @Override
    @Transactional
    public AdminUserCardAdjustResult manualReduceUserCards(Long userId, AdminUserCardManualReduceRequest request) {
        getRequiredUser(userId);
        if (request == null) {
            throw new IllegalArgumentException("request is required");
        }

        List<UserCard> targetCards = resolveCardsToReduce(userId, request);
        if (targetCards.isEmpty()) {
            throw new IllegalArgumentException("no available cards to reduce");
        }

        LocalDateTime now = LocalDateTime.now();
        List<Long> ids = new ArrayList<>(targetCards.size());
        for (UserCard card : targetCards) {
            UserCard update = new UserCard();
            update.setId(card.getId());
            update.setStatus(CARD_STATUS_CANCELLED);
            update.setRemainingTimes(0);
            update.setUpdatedAt(now);
            update.setRemark(buildManualReduceRemark(card.getRemark(), request.getRemark()));
            userCardMapper.updateById(update);
            ids.add(card.getId());
        }

        AdminUserCardAdjustResult result = new AdminUserCardAdjustResult();
        result.setAffectedCount(ids.size());
        result.setUserCardIds(ids);
        return result;
    }

    private UserInfo getRequiredUser(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("userId is required");
        }
        UserInfo user = this.getById(id);
        if (user == null) {
            throw new IllegalArgumentException("user not found");
        }
        return user;
    }

    private void normalizeExpiredMembership(UserInfo user) {
        if (user == null || !Integer.valueOf(1).equals(user.getIsMember())) {
            return;
        }
        if (user.getMemberExpireTime() != null && !user.getMemberExpireTime().isAfter(LocalDateTime.now())) {
            user.setIsMember(0);
            user.setMemberLevel("normal");
            this.updateById(user);
        }
    }

    private UserCard getRequiredUserCard(Long userId, Long cardId) {
        if (cardId == null) {
            throw new IllegalArgumentException("cardId is required");
        }
        UserCard card = userCardMapper.selectOne(
            new LambdaQueryWrapper<UserCard>()
                .eq(UserCard::getId, cardId)
                .eq(UserCard::getUserId, userId)
                .last("limit 1")
        );
        if (card == null) {
            throw new IllegalArgumentException("card not found");
        }
        return card;
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

    private Map<Long, Store> buildStoreMap(
        List<UserStoreWallet> wallets,
        List<UserCard> cards,
        List<WashOrder> orders,
        List<WalletTransaction> transactions,
        List<CardUsageRecord> usages,
        UserDailyDiscountRecord todayDiscountRecord
    ) {
        List<Long> storeIds = List.of(
            wallets.stream().map(UserStoreWallet::getStoreId),
            cards.stream().map(UserCard::getStoreId),
            orders.stream().map(WashOrder::getStoreId),
            transactions.stream().map(WalletTransaction::getStoreId),
            usages.stream().map(CardUsageRecord::getStoreId),
            todayDiscountRecord == null ? java.util.stream.Stream.<Long>empty() : java.util.stream.Stream.of(todayDiscountRecord.getStoreId())
        ).stream()
            .flatMap(Function.identity())
            .filter(storeId -> storeId != null)
            .distinct()
            .toList();

        if (storeIds.isEmpty()) {
            return Map.of();
        }

        return storeService.listByIds(storeIds).stream()
            .collect(Collectors.toMap(Store::getId, Function.identity(), (left, right) -> left));
    }

    private Map<Long, Device> buildDeviceMap(List<WashOrder> orders) {
        List<Long> deviceIds = orders.stream()
            .map(WashOrder::getDeviceId)
            .filter(deviceId -> deviceId != null)
            .distinct()
            .toList();

        if (deviceIds.isEmpty()) {
            return Map.of();
        }

        return deviceService.listByIds(deviceIds).stream()
            .collect(Collectors.toMap(Device::getId, Function.identity(), (left, right) -> left));
    }

    private Map<Long, Store> buildStoreMapFromCards(List<UserCard> cards) {
        List<Long> storeIds = cards.stream()
            .map(UserCard::getStoreId)
            .filter(storeId -> storeId != null)
            .distinct()
            .toList();

        if (storeIds.isEmpty()) {
            return Map.of();
        }

        return storeService.listByIds(storeIds).stream()
            .collect(Collectors.toMap(Store::getId, Function.identity(), (left, right) -> left));
    }

    private Map<Long, Store> buildStoreMapFromUsages(List<CardUsageRecord> usages) {
        List<Long> storeIds = usages.stream()
            .map(CardUsageRecord::getStoreId)
            .filter(storeId -> storeId != null)
            .distinct()
            .toList();

        if (storeIds.isEmpty()) {
            return Map.of();
        }

        return storeService.listByIds(storeIds).stream()
            .collect(Collectors.toMap(Store::getId, Function.identity(), (left, right) -> left));
    }

    private AdminUserWalletAsset toWalletAsset(UserStoreWallet wallet, Map<Long, Store> storeMap) {
        return new AdminUserWalletAsset(
            wallet.getId(),
            wallet.getStoreId(),
            resolveStoreName(storeMap, wallet.getStoreId()),
            normalizeAmount(wallet.getPrincipalBalance()),
            normalizeAmount(wallet.getAvailablePrincipalBalance()),
            normalizeAmount(wallet.getGiftBalance()),
            normalizeAmount(wallet.getAvailableGiftBalance()),
            wallet.getStatus(),
            wallet.getUpdatedAt()
        );
    }

    private AdminUserCardAsset toCardAsset(UserCard card, Map<Long, Store> storeMap) {
        return new AdminUserCardAsset(
            card.getId(),
            card.getStoreId(),
            resolveStoreName(storeMap, card.getStoreId()),
            card.getCardNo(),
            card.getCardType(),
            card.getSourceChannel(),
            card.getTotalTimes(),
            card.getUsedTimes(),
            card.getRemainingTimes(),
            card.getStatus(),
            card.getEffectiveTime(),
            card.getExpireTime(),
            card.getUpdatedAt()
        );
    }

    private AdminUserCardPageItem toCardPageItem(UserCard card, Map<Long, Store> storeMap) {
        return new AdminUserCardPageItem(
            card.getId(),
            card.getUserId(),
            card.getStoreId(),
            resolveStoreName(storeMap, card.getStoreId()),
            card.getCardProductId(),
            card.getCardType(),
            card.getSourceChannel(),
            card.getCardNo(),
            card.getTotalTimes(),
            card.getUsedTimes(),
            card.getRemainingTimes(),
            card.getPurchaseTime(),
            card.getEffectiveTime(),
            card.getExpireTime(),
            card.getStatus(),
            card.getExternalOrderNo(),
            card.getRemark(),
            card.getCreatedAt(),
            card.getUpdatedAt()
        );
    }

    private AdminUserRecentOrder toRecentOrder(WashOrder order, Map<Long, Store> storeMap, Map<Long, Device> deviceMap) {
        Device device = order.getDeviceId() != null ? deviceMap.get(order.getDeviceId()) : null;
        return new AdminUserRecentOrder(
            order.getId(),
            order.getOrderNo(),
            order.getStoreId(),
            resolveStoreName(storeMap, order.getStoreId()),
            order.getDeviceId(),
            device != null ? device.getDeviceName() : "",
            order.getPayMode(),
            order.getPaymentStatus(),
            order.getOrderStatus(),
            normalizeAmount(order.getFinalAmount()),
            normalizeAmount(order.getPaidAmount()),
            order.getCreatedAt()
        );
    }

    private AdminUserWalletTransactionItem toWalletTransactionItem(WalletTransaction transaction, Map<Long, Store> storeMap) {
        return new AdminUserWalletTransactionItem(
            transaction.getId(),
            transaction.getTransactionNo(),
            transaction.getStoreId(),
            resolveStoreName(storeMap, transaction.getStoreId()),
            transaction.getBizType(),
            transaction.getAmountType(),
            transaction.getChangeType(),
            normalizeAmount(transaction.getAmount()),
            normalizeAmount(transaction.getBalanceAfter()),
            transaction.getRelatedOrderNo(),
            transaction.getCreatedAt()
        );
    }

    private AdminUserCardUsageItem toCardUsageItem(CardUsageRecord record, Map<Long, Store> storeMap) {
        return new AdminUserCardUsageItem(
            record.getId(),
            record.getUsageNo(),
            record.getUserCardId(),
            record.getStoreId(),
            resolveStoreName(storeMap, record.getStoreId()),
            record.getOrderId(),
            record.getOrderNo(),
            record.getUsedTimes(),
            record.getUsageTime(),
            record.getCreatedAt()
        );
    }

    private String resolveStoreName(Map<Long, Store> storeMap, Long storeId) {
        if (storeId == null) {
            return "";
        }
        Store store = storeMap.get(storeId);
        return store != null ? store.getStoreName() : "";
    }

    private BigDecimal normalizeAmount(BigDecimal amount) {
        return amount != null ? amount : BigDecimal.ZERO;
    }

    private List<UserCard> resolveCardsToReduce(Long userId, AdminUserCardManualReduceRequest request) {
        List<Long> cardIds = request.getUserCardIds() != null
            ? request.getUserCardIds().stream().filter(id -> id != null).distinct().toList()
            : List.of();

        LambdaQueryWrapper<UserCard> wrapper = new LambdaQueryWrapper<UserCard>()
            .eq(UserCard::getUserId, userId)
            .eq(UserCard::getStatus, CARD_STATUS_ACTIVE)
            .gt(UserCard::getRemainingTimes, 0)
            .orderByAsc(UserCard::getExpireTime)
            .orderByAsc(UserCard::getId);

        if (!cardIds.isEmpty()) {
            wrapper.in(UserCard::getId, cardIds);
            List<UserCard> cards = userCardMapper.selectList(wrapper);
            if (cards.size() != cardIds.size()) {
                throw new IllegalArgumentException("some cards are not available");
            }
            return cards;
        }

        Long storeId = request.getStoreId();
        if (storeId != null) {
            requireStore(storeId);
            wrapper.eq(UserCard::getStoreId, storeId);
        }
        int count = normalizePositiveCount(request.getCount(), "count is required");
        wrapper.last("limit " + count);
        List<UserCard> cards = userCardMapper.selectList(wrapper);
        if (cards.size() < count) {
            throw new IllegalArgumentException("available cards are not enough");
        }
        return cards;
    }

    private int normalizePositiveCount(Integer count, String message) {
        if (count == null || count <= 0) {
            throw new IllegalArgumentException(message);
        }
        if (count > 200) {
            throw new IllegalArgumentException("count cannot exceed 200");
        }
        return count;
    }

    private String normalizeText(String value) {
        return StringUtils.hasText(value) ? value.trim() : "";
    }

    private String buildManualCardNo() {
        return "ADMIN" + UUID.randomUUID().toString().replace("-", "").substring(0, 18);
    }

    private String buildManualAddRemark(String remark) {
        String text = normalizeText(remark);
        return StringUtils.hasText(text) ? "admin manual add: " + text : "admin manual add";
    }

    private String buildManualReduceRemark(String existingRemark, String remark) {
        String text = normalizeText(remark);
        String reduceRemark = StringUtils.hasText(text) ? "admin manual reduce: " + text : "admin manual reduce";
        if (!StringUtils.hasText(existingRemark)) {
            return reduceRemark;
        }
        return existingRemark + " | " + reduceRemark;
    }

    private LocalDateTime parseAdminDateTime(String value, LocalDateTime fallback) {
        if (!StringUtils.hasText(value)) {
            return fallback;
        }
        String text = value.trim();
        try {
            return LocalDateTime.parse(text);
        } catch (DateTimeParseException ignored) {
        }
        try {
            return LocalDateTime.parse(text, ADMIN_DATE_TIME_FORMATTER);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("date time format must be yyyy-MM-dd HH:mm:ss");
        }
    }
}
