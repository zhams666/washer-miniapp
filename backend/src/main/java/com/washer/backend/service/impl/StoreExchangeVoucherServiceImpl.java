package com.washer.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.washer.backend.dto.miniadmin.MiniAdminExchangeVoucherBatchResult;
import com.washer.backend.dto.miniadmin.MiniAdminExchangeVoucherCreateRequest;
import com.washer.backend.dto.miniadmin.MiniAdminExchangeVoucherItem;
import com.washer.backend.dto.miniadmin.MiniAdminSessionContext;
import com.washer.backend.dto.miniadmin.MiniAdminStoreOption;
import com.washer.backend.entity.Store;
import com.washer.backend.entity.StoreExchangeVoucher;
import com.washer.backend.entity.UserInfo;
import com.washer.backend.entity.UserStoreWallet;
import com.washer.backend.entity.WalletTransaction;
import com.washer.backend.mapper.StoreExchangeVoucherMapper;
import com.washer.backend.mapper.StoreMapper;
import com.washer.backend.mapper.UserInfoMapper;
import com.washer.backend.mapper.UserStoreWalletMapper;
import com.washer.backend.mapper.WalletTransactionMapper;
import com.washer.backend.service.MiniAdminAuthService;
import com.washer.backend.service.StoreExchangeVoucherService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class StoreExchangeVoucherServiceImpl implements StoreExchangeVoucherService {

    private static final String STATUS_UNUSED = "unused";
    private static final String STATUS_REDEEMED = "redeemed";
    private static final String PERMISSION_WALLET_ADJUST = "wallet:adjust";
    private static final String PERMISSION_FINANCE_VIEW = "finance:view";
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final StoreExchangeVoucherMapper voucherMapper;
    private final StoreMapper storeMapper;
    private final UserInfoMapper userInfoMapper;
    private final UserStoreWalletMapper userStoreWalletMapper;
    private final WalletTransactionMapper walletTransactionMapper;
    private final MiniAdminAuthService miniAdminAuthService;

    public StoreExchangeVoucherServiceImpl(
        StoreExchangeVoucherMapper voucherMapper,
        StoreMapper storeMapper,
        UserInfoMapper userInfoMapper,
        UserStoreWalletMapper userStoreWalletMapper,
        WalletTransactionMapper walletTransactionMapper,
        MiniAdminAuthService miniAdminAuthService
    ) {
        this.voucherMapper = voucherMapper;
        this.storeMapper = storeMapper;
        this.userInfoMapper = userInfoMapper;
        this.userStoreWalletMapper = userStoreWalletMapper;
        this.walletTransactionMapper = walletTransactionMapper;
        this.miniAdminAuthService = miniAdminAuthService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MiniAdminExchangeVoucherBatchResult createBatch(
        MiniAdminSessionContext context,
        MiniAdminExchangeVoucherCreateRequest request
    ) {
        requirePermission(context, PERMISSION_WALLET_ADJUST);
        if (request == null) {
            throw new IllegalArgumentException("request is required");
        }
        Store store = requireAccessibleStore(context, request.getStoreId());
        BigDecimal amount = normalizePositiveAmount(request.getAmount());
        int count = normalizeCount(request.getCount());
        String batchNo = "EVB" + UUID.randomUUID().toString().replace("-", "").substring(0, 18).toUpperCase();
        LocalDateTime now = LocalDateTime.now();

        for (int index = 0; index < count; index += 1) {
            StoreExchangeVoucher voucher = new StoreExchangeVoucher();
            voucher.setBatchNo(batchNo);
            voucher.setSerialNo(createUniqueSerialNo());
            voucher.setStoreId(store.getId());
            voucher.setAmount(amount);
            voucher.setStatus(STATUS_UNUSED);
            voucher.setCreatedByStaffId(context.getStaff().getId());
            voucher.setCreatedByRoleCode(context.getStaff().getRoleCode());
            voucher.setRemark(truncate(request.getRemark(), 240));
            voucher.setCreatedAt(now);
            voucher.setUpdatedAt(now);
            insertWithRetry(voucher);
        }

        List<MiniAdminExchangeVoucherItem> created = listCreatedBatch(batchNo);
        return new MiniAdminExchangeVoucherBatchResult(
            batchNo,
            store.getId(),
            store.getStoreName(),
            amount,
            count,
            created
        );
    }

    @Override
    public Page<MiniAdminExchangeVoucherItem> pageVouchers(
        MiniAdminSessionContext context,
        long page,
        long size,
        Long storeId,
        String status,
        String keyword
    ) {
        requireViewPermission(context);
        Page<MiniAdminExchangeVoucherItem> empty = new Page<>(Math.max(page, 1), normalizePageSize(size), 0);
        List<Long> limitedStoreIds = limitedStoreIds(context);
        if (!context.isPlatformScope() && limitedStoreIds.isEmpty()) {
            return empty;
        }
        if (storeId != null && storeId > 0) {
            requireAccessibleStore(context, storeId);
        }

        LambdaQueryWrapper<StoreExchangeVoucher> wrapper = new LambdaQueryWrapper<StoreExchangeVoucher>()
            .eq(storeId != null && storeId > 0, StoreExchangeVoucher::getStoreId, storeId)
            .in(storeId == null && !context.isPlatformScope(), StoreExchangeVoucher::getStoreId, limitedStoreIds)
            .eq(StringUtils.hasText(status), StoreExchangeVoucher::getStatus, normalizeStatus(status))
            .orderByDesc(StoreExchangeVoucher::getId);

        if (StringUtils.hasText(keyword)) {
            String value = normalizeSerialNo(keyword);
            wrapper.and(w -> w
                .like(StoreExchangeVoucher::getSerialNo, value)
                .or()
                .like(StoreExchangeVoucher::getBatchNo, value));
        }

        Page<StoreExchangeVoucher> voucherPage = voucherMapper.selectPage(
            new Page<>(Math.max(page, 1), normalizePageSize(size)),
            wrapper
        );
        Map<Long, Store> storeMap = buildStoreMap(voucherPage.getRecords());
        Map<Long, UserInfo> userMap = buildUserMap(voucherPage.getRecords());
        Map<Long, WalletTransaction> transactionMap = buildTransactionMap(voucherPage.getRecords());

        Page<MiniAdminExchangeVoucherItem> result = new Page<>(
            voucherPage.getCurrent(),
            voucherPage.getSize(),
            voucherPage.getTotal()
        );
        result.setRecords(voucherPage.getRecords().stream()
            .map(voucher -> toItem(voucher, storeMap, userMap, transactionMap))
            .toList());
        return result;
    }

    @Override
    public Page<MiniAdminExchangeVoucherItem> pageAdminVouchers(
        long page,
        long size,
        Long storeId,
        String status,
        String keyword
    ) {
        LambdaQueryWrapper<StoreExchangeVoucher> wrapper = new LambdaQueryWrapper<StoreExchangeVoucher>()
            .eq(storeId != null && storeId > 0, StoreExchangeVoucher::getStoreId, storeId)
            .eq(StringUtils.hasText(status), StoreExchangeVoucher::getStatus, normalizeStatus(status))
            .orderByDesc(StoreExchangeVoucher::getId);

        if (StringUtils.hasText(keyword)) {
            String value = normalizeSerialNo(keyword);
            wrapper.and(w -> w
                .like(StoreExchangeVoucher::getSerialNo, value)
                .or()
                .like(StoreExchangeVoucher::getBatchNo, value));
        }

        Page<StoreExchangeVoucher> voucherPage = voucherMapper.selectPage(
            new Page<>(Math.max(page, 1), normalizePageSize(size)),
            wrapper
        );
        Map<Long, Store> storeMap = buildStoreMap(voucherPage.getRecords());
        Map<Long, UserInfo> userMap = buildUserMap(voucherPage.getRecords());
        Map<Long, WalletTransaction> transactionMap = buildTransactionMap(voucherPage.getRecords());

        Page<MiniAdminExchangeVoucherItem> result = new Page<>(
            voucherPage.getCurrent(),
            voucherPage.getSize(),
            voucherPage.getTotal()
        );
        result.setRecords(voucherPage.getRecords().stream()
            .map(voucher -> toItem(voucher, storeMap, userMap, transactionMap))
            .toList());
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> redeem(Long userId, Long storeId, String serialNo) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("userId is required");
        }
        String normalizedSerialNo = normalizeSerialNo(serialNo);
        if (!StringUtils.hasText(normalizedSerialNo)) {
            throw new IllegalArgumentException("serialNo is required");
        }

        UserInfo user = userInfoMapper.selectById(userId);
        if (user == null) {
            throw new IllegalArgumentException("user not found");
        }

        StoreExchangeVoucher voucher = voucherMapper.selectOne(
            new LambdaQueryWrapper<StoreExchangeVoucher>()
                .eq(StoreExchangeVoucher::getSerialNo, normalizedSerialNo)
                .last("limit 1 for update")
        );
        if (voucher == null) {
            throw new IllegalArgumentException("exchange voucher not found");
        }
        if (storeId != null && storeId > 0 && !storeId.equals(voucher.getStoreId())) {
            throw new IllegalArgumentException("exchange voucher does not belong to store");
        }
        if (!STATUS_UNUSED.equals(normalizeStatus(voucher.getStatus()))) {
            throw new IllegalArgumentException("exchange voucher already redeemed");
        }

        Store store = requireStore(voucher.getStoreId());
        UserStoreWallet wallet = loadOrCreateWalletForUpdate(userId, store.getId());
        WalletBalanceResult balanceResult = applyGiftBalance(wallet, voucher.getAmount());
        WalletTransaction transaction = insertWalletTransaction(
            userId,
            store.getId(),
            normalizedSerialNo,
            normalizeAmount(voucher.getAmount()),
            balanceResult.before(),
            balanceResult.after()
        );
        LocalDateTime now = LocalDateTime.now();
        int updated = voucherMapper.update(
            null,
            new LambdaUpdateWrapper<StoreExchangeVoucher>()
                .eq(StoreExchangeVoucher::getId, voucher.getId())
                .eq(StoreExchangeVoucher::getStatus, STATUS_UNUSED)
                .set(StoreExchangeVoucher::getStatus, STATUS_REDEEMED)
                .set(StoreExchangeVoucher::getRedeemedUserId, userId)
                .set(StoreExchangeVoucher::getRedeemedWalletTransactionId, transaction.getId())
                .set(StoreExchangeVoucher::getRedeemedAt, now)
                .set(StoreExchangeVoucher::getUpdatedAt, now)
        );
        if (updated <= 0) {
            throw new IllegalStateException("exchange voucher changed while redeeming");
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("serialNo", normalizedSerialNo);
        result.put("storeId", store.getId());
        result.put("storeName", store.getStoreName());
        result.put("amount", normalizeAmount(voucher.getAmount()));
        result.put("balanceBefore", balanceResult.before());
        result.put("balanceAfter", balanceResult.after());
        result.put("transactionNo", transaction.getTransactionNo());
        result.put("redeemedAt", now);
        return result;
    }

    private void insertWithRetry(StoreExchangeVoucher voucher) {
        for (int attempt = 0; attempt < 5; attempt += 1) {
            try {
                voucherMapper.insert(voucher);
                return;
            } catch (DuplicateKeyException ex) {
                voucher.setSerialNo(createUniqueSerialNo());
            }
        }
        throw new IllegalStateException("failed to generate unique exchange voucher serial number");
    }

    private List<MiniAdminExchangeVoucherItem> listCreatedBatch(String batchNo) {
        List<StoreExchangeVoucher> vouchers = voucherMapper.selectList(
            new LambdaQueryWrapper<StoreExchangeVoucher>()
                .eq(StoreExchangeVoucher::getBatchNo, batchNo)
                .orderByAsc(StoreExchangeVoucher::getId)
        );
        Map<Long, Store> storeMap = buildStoreMap(vouchers);
        Map<Long, UserInfo> userMap = buildUserMap(vouchers);
        Map<Long, WalletTransaction> transactionMap = buildTransactionMap(vouchers);
        return vouchers.stream()
            .map(voucher -> toItem(voucher, storeMap, userMap, transactionMap))
            .toList();
    }

    private String createUniqueSerialNo() {
        for (int attempt = 0; attempt < 10; attempt += 1) {
            String serialNo = buildSerialNo();
            Long count = voucherMapper.selectCount(
                new LambdaQueryWrapper<StoreExchangeVoucher>()
                    .eq(StoreExchangeVoucher::getSerialNo, serialNo)
            );
            if (count == null || count <= 0) {
                return serialNo;
            }
        }
        throw new IllegalStateException("failed to generate unique exchange voucher serial number");
    }

    private String buildSerialNo() {
        StringBuilder builder = new StringBuilder("EV");
        for (int index = 0; index < 12; index += 1) {
            builder.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return builder.toString();
    }

    private Store requireAccessibleStore(MiniAdminSessionContext context, Long storeId) {
        if (context == null) {
            throw new IllegalArgumentException("管理端登录已失效，请重新登录");
        }
        if (storeId == null || storeId <= 0) {
            throw new IllegalArgumentException("storeId is required");
        }
        if (!miniAdminAuthService.canAccessStore(context, storeId)) {
            throw new IllegalArgumentException("无权访问该门店");
        }
        return requireStore(storeId);
    }

    private Store requireStore(Long storeId) {
        Store store = storeId != null ? storeMapper.selectById(storeId) : null;
        if (store == null) {
            throw new IllegalArgumentException("store not found");
        }
        return store;
    }

    private List<Long> limitedStoreIds(MiniAdminSessionContext context) {
        if (context == null || context.isPlatformScope()) {
            return List.of();
        }
        return context.getStores().stream()
            .map(MiniAdminStoreOption::getId)
            .filter(id -> id != null && id > 0)
            .distinct()
            .toList();
    }

    private UserStoreWallet loadOrCreateWalletForUpdate(Long userId, Long storeId) {
        UserStoreWallet wallet = userStoreWalletMapper.selectOne(
            new LambdaQueryWrapper<UserStoreWallet>()
                .eq(UserStoreWallet::getUserId, userId)
                .eq(UserStoreWallet::getStoreId, storeId)
                .last("limit 1 for update")
        );
        if (wallet != null) {
            return wallet;
        }
        wallet = new UserStoreWallet();
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
        try {
            userStoreWalletMapper.insert(wallet);
        } catch (DuplicateKeyException ex) {
            wallet = userStoreWalletMapper.selectOne(
                new LambdaQueryWrapper<UserStoreWallet>()
                    .eq(UserStoreWallet::getUserId, userId)
                    .eq(UserStoreWallet::getStoreId, storeId)
                    .last("limit 1 for update")
            );
        }
        if (wallet == null) {
            throw new IllegalStateException("wallet create failed");
        }
        return wallet;
    }

    private WalletBalanceResult applyGiftBalance(UserStoreWallet wallet, BigDecimal amount) {
        BigDecimal safeAmount = normalizeAmount(amount);
        BigDecimal giftBalance = resolveAmount(wallet.getGiftBalance());
        BigDecimal availableGiftBalance = wallet.getAvailableGiftBalance() != null
            ? wallet.getAvailableGiftBalance()
            : giftBalance;
        BigDecimal afterGift = giftBalance.add(safeAmount);
        BigDecimal afterAvailableGift = availableGiftBalance.add(safeAmount);
        int updated = userStoreWalletMapper.update(
            null,
            new LambdaUpdateWrapper<UserStoreWallet>()
                .eq(UserStoreWallet::getId, wallet.getId())
                .set(UserStoreWallet::getGiftBalance, afterGift)
                .set(UserStoreWallet::getAvailableGiftBalance, afterAvailableGift)
                .set(UserStoreWallet::getTotalRechargeGift, resolveAmount(wallet.getTotalRechargeGift()).add(safeAmount))
                .set(UserStoreWallet::getUpdatedAt, LocalDateTime.now())
        );
        if (updated <= 0) {
            throw new IllegalStateException("wallet changed while redeeming exchange voucher");
        }
        return new WalletBalanceResult(availableGiftBalance, afterAvailableGift);
    }

    private WalletTransaction insertWalletTransaction(
        Long userId,
        Long storeId,
        String serialNo,
        BigDecimal amount,
        BigDecimal balanceBefore,
        BigDecimal balanceAfter
    ) {
        WalletTransaction transaction = new WalletTransaction();
        transaction.setTransactionNo("WT" + UUID.randomUUID().toString().replace("-", "").substring(0, 18));
        transaction.setUserId(userId);
        transaction.setStoreId(storeId);
        transaction.setBizType("exchange_voucher");
        transaction.setAmountType("gift");
        transaction.setBalanceBucket("available");
        transaction.setChangeType("in");
        transaction.setAmount(amount);
        transaction.setRelatedAction("exchange_voucher_redeem");
        transaction.setBizActionNo("EVR_" + serialNo);
        transaction.setBalanceBefore(balanceBefore);
        transaction.setBalanceAfter(balanceAfter);
        transaction.setRelatedOrderNo(serialNo);
        transaction.setRemark("门店兑换券核销");
        transaction.setCreatedAt(LocalDateTime.now());
        walletTransactionMapper.insert(transaction);
        return transaction;
    }

    private Map<Long, Store> buildStoreMap(List<StoreExchangeVoucher> vouchers) {
        List<Long> storeIds = vouchers.stream()
            .map(StoreExchangeVoucher::getStoreId)
            .filter(id -> id != null)
            .distinct()
            .toList();
        if (storeIds.isEmpty()) {
            return Map.of();
        }
        return storeMapper.selectBatchIds(storeIds).stream()
            .collect(Collectors.toMap(Store::getId, Function.identity(), (left, right) -> left));
    }

    private Map<Long, UserInfo> buildUserMap(List<StoreExchangeVoucher> vouchers) {
        Set<Long> userIds = vouchers.stream()
            .map(StoreExchangeVoucher::getRedeemedUserId)
            .filter(id -> id != null && id > 0)
            .collect(Collectors.toSet());
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return userInfoMapper.selectBatchIds(userIds).stream()
            .collect(Collectors.toMap(UserInfo::getId, Function.identity(), (left, right) -> left));
    }

    private Map<Long, WalletTransaction> buildTransactionMap(List<StoreExchangeVoucher> vouchers) {
        Set<Long> transactionIds = vouchers.stream()
            .map(StoreExchangeVoucher::getRedeemedWalletTransactionId)
            .filter(id -> id != null && id > 0)
            .collect(Collectors.toSet());
        if (transactionIds.isEmpty()) {
            return Map.of();
        }
        return walletTransactionMapper.selectBatchIds(transactionIds).stream()
            .collect(Collectors.toMap(WalletTransaction::getId, Function.identity(), (left, right) -> left));
    }

    private MiniAdminExchangeVoucherItem toItem(
        StoreExchangeVoucher voucher,
        Map<Long, Store> storeMap,
        Map<Long, UserInfo> userMap,
        Map<Long, WalletTransaction> transactionMap
    ) {
        Store store = storeMap.get(voucher.getStoreId());
        UserInfo user = voucher.getRedeemedUserId() != null
            ? userMap.get(voucher.getRedeemedUserId())
            : null;
        WalletTransaction transaction = voucher.getRedeemedWalletTransactionId() != null
            ? transactionMap.get(voucher.getRedeemedWalletTransactionId())
            : null;
        return new MiniAdminExchangeVoucherItem(
            voucher.getId(),
            voucher.getBatchNo(),
            voucher.getSerialNo(),
            voucher.getStoreId(),
            store != null ? store.getStoreName() : "",
            normalizeAmount(voucher.getAmount()),
            normalizeStatus(voucher.getStatus()),
            voucher.getRedeemedUserId(),
            user != null ? user.getNickname() : "",
            user != null ? user.getMobile() : "",
            transaction != null ? transaction.getTransactionNo() : "",
            voucher.getRedeemedAt(),
            voucher.getRemark(),
            voucher.getCreatedAt()
        );
    }

    private void requirePermission(MiniAdminSessionContext context, String permission) {
        if (context == null || !miniAdminAuthService.hasPermission(context, permission)) {
            throw new IllegalArgumentException("无兑换券操作权限");
        }
    }

    private void requireViewPermission(MiniAdminSessionContext context) {
        if (context == null) {
            throw new IllegalArgumentException("管理端登录已失效，请重新登录");
        }
        if (miniAdminAuthService.hasPermission(context, PERMISSION_WALLET_ADJUST)
            || miniAdminAuthService.hasPermission(context, PERMISSION_FINANCE_VIEW)
            || miniAdminAuthService.hasPermission(context, "user:view")) {
            return;
        }
        throw new IllegalArgumentException("无兑换券查看权限");
    }

    private BigDecimal normalizePositiveAmount(BigDecimal amount) {
        BigDecimal value = normalizeAmount(amount);
        if (value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amount must be > 0");
        }
        if (value.compareTo(new BigDecimal("9999.99")) > 0) {
            throw new IllegalArgumentException("amount cannot exceed 9999.99");
        }
        return value;
    }

    private BigDecimal normalizeAmount(BigDecimal amount) {
        return (amount != null ? amount : BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal resolveAmount(BigDecimal amount) {
        return amount != null ? amount : BigDecimal.ZERO;
    }

    private int normalizeCount(Integer count) {
        int value = count != null ? count : 0;
        if (value <= 0) {
            throw new IllegalArgumentException("count must be > 0");
        }
        if (value > 500) {
            throw new IllegalArgumentException("count cannot exceed 500");
        }
        return value;
    }

    private long normalizePageSize(long size) {
        return Math.max(1, Math.min(size, 100));
    }

    private String normalizeSerialNo(String value) {
        return StringUtils.hasText(value)
            ? value.trim().replace(" ", "").replace("-", "").toUpperCase()
            : "";
    }

    private String normalizeStatus(String value) {
        return StringUtils.hasText(value) ? value.trim().toLowerCase() : "";
    }

    private String truncate(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String text = value.trim();
        return text.length() <= maxLength ? text : text.substring(0, maxLength);
    }

    private record WalletBalanceResult(BigDecimal before, BigDecimal after) {
    }
}
