package com.washer.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.washer.backend.dto.admin.AdminUserAssetOverview;
import com.washer.backend.dto.admin.AdminUserCardAsset;
import com.washer.backend.dto.admin.AdminUserCardUsageItem;
import com.washer.backend.dto.admin.AdminUserListItem;
import com.washer.backend.dto.admin.AdminUserRecentOrder;
import com.washer.backend.dto.admin.AdminUserWalletAsset;
import com.washer.backend.dto.admin.AdminUserWalletTransactionItem;
import com.washer.backend.entity.CardUsageRecord;
import com.washer.backend.entity.Device;
import com.washer.backend.entity.Store;
import com.washer.backend.entity.UserCard;
import com.washer.backend.entity.UserDailyDiscountRecord;
import com.washer.backend.entity.UserInfo;
import com.washer.backend.entity.UserStoreWallet;
import com.washer.backend.entity.WalletTransaction;
import com.washer.backend.entity.WashOrder;
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
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo> implements UserInfoService {

    private static final String DISCOUNT_TYPE_FIRST_PERIOD = "first_period_discount";

    private final StoreService storeService;
    private final DeviceService deviceService;
    private final UserStoreWalletMapper userStoreWalletMapper;
    private final UserCardMapper userCardMapper;
    private final WalletTransactionMapper walletTransactionMapper;
    private final CardUsageRecordMapper cardUsageRecordMapper;
    private final UserDailyDiscountRecordMapper userDailyDiscountRecordMapper;
    private final WashOrderMapper washOrderMapper;

    public UserInfoServiceImpl(
        StoreService storeService,
        DeviceService deviceService,
        UserStoreWalletMapper userStoreWalletMapper,
        UserCardMapper userCardMapper,
        WalletTransactionMapper walletTransactionMapper,
        CardUsageRecordMapper cardUsageRecordMapper,
        UserDailyDiscountRecordMapper userDailyDiscountRecordMapper,
        WashOrderMapper washOrderMapper
    ) {
        this.storeService = storeService;
        this.deviceService = deviceService;
        this.userStoreWalletMapper = userStoreWalletMapper;
        this.userCardMapper = userCardMapper;
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
            .map(user -> new AdminUserListItem(
                user.getId(),
                user.getUserNo(),
                user.getNickname(),
                user.getRealName(),
                user.getMobile(),
                user.getUserStatus(),
                user.getRegisterSource(),
                user.getIsMember(),
                user.getMemberLevel(),
                user.getLastConsumeTime(),
                user.getCreatedAt()
            ))
            .toList());
        return result;
    }

    @Override
    public AdminUserAssetOverview getAdminUserAssetOverview(Long id) {
        UserInfo user = getRequiredUser(id);
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
            user.getMemberSinceTime(),
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

    private UserInfo getRequiredUser(Long id) {
        UserInfo user = this.getById(id);
        if (user == null) {
            throw new IllegalArgumentException("user not found");
        }
        return user;
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
}
