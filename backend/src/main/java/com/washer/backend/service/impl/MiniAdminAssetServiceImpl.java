package com.washer.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.washer.backend.dto.miniadmin.MiniAdminAssetOperationResult;
import com.washer.backend.dto.miniadmin.MiniAdminCardAdjustmentRequest;
import com.washer.backend.dto.miniadmin.MiniAdminSessionContext;
import com.washer.backend.dto.miniadmin.MiniAdminStoreOption;
import com.washer.backend.dto.miniadmin.MiniAdminUserAssetSummary;
import com.washer.backend.dto.miniadmin.MiniAdminUserCardItem;
import com.washer.backend.dto.miniadmin.MiniAdminUserSearchItem;
import com.washer.backend.dto.miniadmin.MiniAdminWalletAdjustmentRequest;
import com.washer.backend.dto.miniadmin.MiniAdminWalletFineRequest;
import com.washer.backend.entity.CardUsageRecord;
import com.washer.backend.entity.MiniAdminAssetOperation;
import com.washer.backend.entity.Store;
import com.washer.backend.entity.UserCard;
import com.washer.backend.entity.UserInfo;
import com.washer.backend.entity.UserStoreWallet;
import com.washer.backend.entity.WalletTransaction;
import com.washer.backend.mapper.CardUsageRecordMapper;
import com.washer.backend.mapper.MiniAdminAssetOperationMapper;
import com.washer.backend.mapper.StoreMapper;
import com.washer.backend.mapper.UserCardMapper;
import com.washer.backend.mapper.UserInfoMapper;
import com.washer.backend.mapper.UserStoreWalletMapper;
import com.washer.backend.mapper.WalletTransactionMapper;
import com.washer.backend.service.MiniAdminAssetService;
import com.washer.backend.service.MiniAdminAuthService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class MiniAdminAssetServiceImpl implements MiniAdminAssetService {

    private static final String PERMISSION_WALLET_ADJUST = "wallet:adjust";
    private static final String PERMISSION_CARD_ADJUST = "card:adjust";
    private static final String CHANGE_IN = "in";
    private static final String CHANGE_OUT = "out";
    private static final String OP_WALLET_ADJUST = "wallet_adjust";
    private static final String OP_FINE = "fine";
    private static final String OP_CARD_ADJUST = "card_adjust";

    private final MiniAdminAuthService miniAdminAuthService;
    private final UserInfoMapper userInfoMapper;
    private final StoreMapper storeMapper;
    private final UserStoreWalletMapper userStoreWalletMapper;
    private final WalletTransactionMapper walletTransactionMapper;
    private final UserCardMapper userCardMapper;
    private final CardUsageRecordMapper cardUsageRecordMapper;
    private final MiniAdminAssetOperationMapper assetOperationMapper;

    public MiniAdminAssetServiceImpl(
        MiniAdminAuthService miniAdminAuthService,
        UserInfoMapper userInfoMapper,
        StoreMapper storeMapper,
        UserStoreWalletMapper userStoreWalletMapper,
        WalletTransactionMapper walletTransactionMapper,
        UserCardMapper userCardMapper,
        CardUsageRecordMapper cardUsageRecordMapper,
        MiniAdminAssetOperationMapper assetOperationMapper
    ) {
        this.miniAdminAuthService = miniAdminAuthService;
        this.userInfoMapper = userInfoMapper;
        this.storeMapper = storeMapper;
        this.userStoreWalletMapper = userStoreWalletMapper;
        this.walletTransactionMapper = walletTransactionMapper;
        this.userCardMapper = userCardMapper;
        this.cardUsageRecordMapper = cardUsageRecordMapper;
        this.assetOperationMapper = assetOperationMapper;
    }

    @Override
    public List<MiniAdminUserSearchItem> searchUsers(MiniAdminSessionContext context, Long storeId, String keyword) {
        requireAssetPermission(context);
        List<Long> storeIds = resolveAccessibleStoreIds(context, storeId);
        if (storeIds.isEmpty()) {
            return List.of();
        }

        LambdaQueryWrapper<UserInfo> wrapper = new LambdaQueryWrapper<UserInfo>()
            .orderByDesc(UserInfo::getId)
            .last("limit 20");
        if (StringUtils.hasText(keyword)) {
            String value = keyword.trim();
            Long numericId = parseLong(value);
            wrapper.and(w -> {
                w.like(UserInfo::getNickname, value)
                    .or()
                    .like(UserInfo::getRealName, value)
                    .or()
                    .like(UserInfo::getMobile, value)
                    .or()
                    .like(UserInfo::getUserNo, value);
                if (numericId != null) {
                    w.or().eq(UserInfo::getId, numericId);
                }
            });
        }

        List<UserInfo> users = userInfoMapper.selectList(wrapper);
        if (users.isEmpty()) {
            return List.of();
        }
        List<Long> userIds = users.stream().map(UserInfo::getId).toList();
        Map<Long, WalletTotals> walletTotals = buildWalletTotals(userIds, storeIds);
        Map<Long, Integer> cardTotals = buildCardTotals(userIds, storeIds);

        return users.stream()
            .map(user -> {
                WalletTotals totals = walletTotals.getOrDefault(user.getId(), WalletTotals.ZERO);
                return new MiniAdminUserSearchItem(
                    user.getId(),
                    user.getUserNo(),
                    user.getNickname(),
                    user.getRealName(),
                    user.getMobile(),
                    user.getAvatarUrl(),
                    user.getUserStatus(),
                    totals.principal(),
                    totals.gift(),
                    cardTotals.getOrDefault(user.getId(), 0)
                );
            })
            .toList();
    }

    @Override
    public MiniAdminUserAssetSummary getUserAssetSummary(MiniAdminSessionContext context, Long userId, Long storeId) {
        requireAssetPermission(context);
        UserInfo user = requireUser(userId);
        Store store = requireAccessibleStore(context, storeId);
        UserStoreWallet wallet = findWallet(user.getId(), store.getId(), false);
        List<UserCard> cards = loadCards(user.getId(), store.getId());
        int remainingCardTimes = cards.stream()
            .map(UserCard::getRemainingTimes)
            .filter(times -> times != null && times > 0)
            .reduce(0, Integer::sum);
        return new MiniAdminUserAssetSummary(
            user.getId(),
            user.getUserNo(),
            user.getNickname(),
            user.getRealName(),
            user.getMobile(),
            store.getId(),
            store.getStoreName(),
            wallet != null ? wallet.getId() : null,
            resolveAvailablePrincipal(wallet),
            resolveAvailableGift(wallet),
            resolveAvailablePrincipal(wallet).add(resolveAvailableGift(wallet)),
            remainingCardTimes,
            cards.stream().map(card -> toCardItem(card, store)).toList()
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MiniAdminAssetOperationResult adjustWallet(
        MiniAdminSessionContext context,
        MiniAdminWalletAdjustmentRequest request
    ) {
        ensurePermission(context, PERMISSION_WALLET_ADJUST);
        if (request == null) {
            throw new IllegalArgumentException("request is required");
        }
        UserInfo user = requireUser(request.getUserId());
        Store store = requireAccessibleStore(context, request.getStoreId());
        String changeType = normalizeChangeType(request.getChangeType());
        BigDecimal principalAmount = normalizePositiveAmount(request.getPrincipalAmount());
        BigDecimal giftAmount = normalizePositiveAmount(request.getGiftAmount());
        if (principalAmount.add(giftAmount).compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("principalAmount or giftAmount is required");
        }
        String remark = requireRemark(request.getRemark());

        UserStoreWallet wallet = findWallet(user.getId(), store.getId(), true);
        applyWalletDelta(wallet, changeType, principalAmount, giftAmount);
        String operationNo = buildOperationNo("WA");
        if (principalAmount.compareTo(BigDecimal.ZERO) > 0) {
            insertWalletTransaction(
                user.getId(),
                store.getId(),
                "manual_adjust",
                "wallet_adjust",
                "principal",
                changeType,
                principalAmount,
                operationNo,
                buildWalletAdjustRemark(changeType, "通用余额", remark)
            );
        }
        if (giftAmount.compareTo(BigDecimal.ZERO) > 0) {
            insertWalletTransaction(
                user.getId(),
                store.getId(),
                "manual_adjust",
                "wallet_adjust",
                "gift",
                changeType,
                giftAmount,
                operationNo,
                buildWalletAdjustRemark(changeType, "赠送余额", remark)
            );
        }
        insertAssetOperation(
            operationNo,
            OP_WALLET_ADJUST,
            changeType,
            context,
            user.getId(),
            store.getId(),
            wallet.getId(),
            null,
            resolveAmountType(principalAmount, giftAmount),
            principalAmount,
            giftAmount,
            0,
            remark
        );
        return buildResult(operationNo, OP_WALLET_ADJUST, user.getId(), store.getId(), wallet.getId(), null, principalAmount, giftAmount, 0, remark);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MiniAdminAssetOperationResult createFine(MiniAdminSessionContext context, MiniAdminWalletFineRequest request) {
        ensurePermission(context, PERMISSION_WALLET_ADJUST);
        if (request == null) {
            throw new IllegalArgumentException("request is required");
        }
        UserInfo user = requireUser(request.getUserId());
        Store store = requireAccessibleStore(context, request.getStoreId());
        BigDecimal amount = normalizePositiveAmount(request.getAmount());
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amount must be > 0");
        }
        String remark = requireRemark(request.getRemark());
        UserStoreWallet wallet = findWallet(user.getId(), store.getId(), true);

        BigDecimal availablePrincipal = resolveAvailablePrincipal(wallet);
        BigDecimal availableGift = resolveAvailableGift(wallet);
        if (availablePrincipal.add(availableGift).compareTo(amount) < 0) {
            throw new IllegalArgumentException("wallet balance is not enough");
        }
        BigDecimal principalAmount = amount.min(availablePrincipal);
        BigDecimal giftAmount = amount.subtract(principalAmount);
        applyWalletDelta(wallet, CHANGE_OUT, principalAmount, giftAmount);

        String operationNo = buildOperationNo("FN");
        if (principalAmount.compareTo(BigDecimal.ZERO) > 0) {
            insertWalletTransaction(
                user.getId(),
                store.getId(),
                "fine",
                "fine",
                "principal",
                CHANGE_OUT,
                principalAmount,
                operationNo,
                "罚款：" + remark
            );
        }
        if (giftAmount.compareTo(BigDecimal.ZERO) > 0) {
            insertWalletTransaction(
                user.getId(),
                store.getId(),
                "fine",
                "fine",
                "gift",
                CHANGE_OUT,
                giftAmount,
                operationNo,
                "罚款：" + remark
            );
        }
        insertAssetOperation(
            operationNo,
            OP_FINE,
            CHANGE_OUT,
            context,
            user.getId(),
            store.getId(),
            wallet.getId(),
            null,
            resolveAmountType(principalAmount, giftAmount),
            principalAmount,
            giftAmount,
            0,
            remark
        );
        return buildResult(operationNo, OP_FINE, user.getId(), store.getId(), wallet.getId(), null, principalAmount, giftAmount, 0, remark);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MiniAdminAssetOperationResult adjustCard(MiniAdminSessionContext context, MiniAdminCardAdjustmentRequest request) {
        ensurePermission(context, PERMISSION_CARD_ADJUST);
        if (request == null) {
            throw new IllegalArgumentException("request is required");
        }
        UserInfo user = requireUser(request.getUserId());
        Store store = requireAccessibleStore(context, request.getStoreId());
        int deltaTimes = request.getDeltaTimes() != null ? request.getDeltaTimes() : 0;
        if (deltaTimes == 0) {
            throw new IllegalArgumentException("deltaTimes is required");
        }
        String remark = requireRemark(request.getRemark());
        String operationNo = buildOperationNo("CA");
        Long touchedCardId;
        if (deltaTimes > 0) {
            touchedCardId = addCardTimes(user.getId(), store.getId(), request.getUserCardId(), deltaTimes, operationNo, remark);
        } else {
            touchedCardId = deductCardTimes(context, user.getId(), store.getId(), request.getUserCardId(), -deltaTimes, operationNo, remark);
        }
        insertAssetOperation(
            operationNo,
            OP_CARD_ADJUST,
            deltaTimes > 0 ? CHANGE_IN : CHANGE_OUT,
            context,
            user.getId(),
            store.getId(),
            null,
            touchedCardId,
            null,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            deltaTimes,
            remark
        );
        return buildResult(operationNo, OP_CARD_ADJUST, user.getId(), store.getId(), null, touchedCardId, BigDecimal.ZERO, BigDecimal.ZERO, deltaTimes, remark);
    }

    private void applyWalletDelta(UserStoreWallet wallet, String changeType, BigDecimal principalAmount, BigDecimal giftAmount) {
        BigDecimal principalBalance = resolvePrincipalBalance(wallet);
        BigDecimal principalAvailable = resolveAvailablePrincipal(wallet);
        BigDecimal giftBalance = resolveGiftBalance(wallet);
        BigDecimal giftAvailable = resolveAvailableGift(wallet);

        BigDecimal sign = CHANGE_IN.equals(changeType) ? BigDecimal.ONE : BigDecimal.ONE.negate();
        BigDecimal principalAfter = principalBalance.add(principalAmount.multiply(sign));
        BigDecimal principalAvailableAfter = principalAvailable.add(principalAmount.multiply(sign));
        BigDecimal giftAfter = giftBalance.add(giftAmount.multiply(sign));
        BigDecimal giftAvailableAfter = giftAvailable.add(giftAmount.multiply(sign));
        if (principalAfter.compareTo(BigDecimal.ZERO) < 0 || principalAvailableAfter.compareTo(BigDecimal.ZERO) < 0
            || giftAfter.compareTo(BigDecimal.ZERO) < 0 || giftAvailableAfter.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("wallet balance is not enough");
        }

        LambdaUpdateWrapper<UserStoreWallet> wrapper = new LambdaUpdateWrapper<UserStoreWallet>()
            .eq(UserStoreWallet::getId, wallet.getId())
            .set(UserStoreWallet::getPrincipalBalance, principalAfter)
            .set(UserStoreWallet::getAvailablePrincipalBalance, principalAvailableAfter)
            .set(UserStoreWallet::getGiftBalance, giftAfter)
            .set(UserStoreWallet::getAvailableGiftBalance, giftAvailableAfter);
        if (CHANGE_IN.equals(changeType)) {
            wrapper
                .set(UserStoreWallet::getTotalRechargePrincipal, resolveAmount(wallet.getTotalRechargePrincipal()).add(principalAmount))
                .set(UserStoreWallet::getTotalRechargeGift, resolveAmount(wallet.getTotalRechargeGift()).add(giftAmount));
        } else {
            wrapper
                .set(UserStoreWallet::getTotalConsumePrincipal, resolveAmount(wallet.getTotalConsumePrincipal()).add(principalAmount))
                .set(UserStoreWallet::getTotalConsumeGift, resolveAmount(wallet.getTotalConsumeGift()).add(giftAmount));
        }
        int updated = userStoreWalletMapper.update(null, wrapper);
        if (updated <= 0) {
            throw new IllegalStateException("wallet changed while applying operation");
        }
    }

    private Long addCardTimes(Long userId, Long storeId, Long userCardId, int deltaTimes, String operationNo, String remark) {
        UserCard card = userCardId != null ? requireUserCard(userCardId, userId, storeId, true) : null;
        if (card == null) {
            card = createManualCard(userId, storeId, deltaTimes, operationNo, remark);
        } else {
            int totalTimes = safeInt(card.getTotalTimes()) + deltaTimes;
            int remainingTimes = safeInt(card.getRemainingTimes()) + deltaTimes;
            int updated = userCardMapper.update(
                null,
                new LambdaUpdateWrapper<UserCard>()
                    .eq(UserCard::getId, card.getId())
                    .set(UserCard::getTotalTimes, totalTimes)
                    .set(UserCard::getRemainingTimes, remainingTimes)
                    .set(UserCard::getStatus, "active")
                    .set(UserCard::getRemark, appendRemark(card.getRemark(), "人工加次：" + remark))
            );
            if (updated <= 0) {
                throw new IllegalStateException("card changed while applying operation");
            }
        }
        insertCardUsageRecord(card.getId(), userId, storeId, -deltaTimes, operationNo, "人工加次：" + remark);
        return card.getId();
    }

    private Long deductCardTimes(
        MiniAdminSessionContext context,
        Long userId,
        Long storeId,
        Long userCardId,
        int deductTimes,
        String operationNo,
        String remark
    ) {
        int remainingToDeduct = deductTimes;
        Long touchedCardId = null;
        List<UserCard> cards = userCardId != null
            ? List.of(requireUserCard(userCardId, userId, storeId, true))
            : loadAvailableCardsForUpdate(userId, storeId);
        int availableTimes = cards.stream().map(UserCard::getRemainingTimes).filter(times -> times != null && times > 0).reduce(0, Integer::sum);
        if (availableTimes < deductTimes) {
            throw new IllegalArgumentException("card remaining times is not enough");
        }
        for (UserCard card : cards) {
            if (remainingToDeduct <= 0) {
                break;
            }
            int currentRemaining = Math.max(safeInt(card.getRemainingTimes()), 0);
            if (currentRemaining <= 0) {
                continue;
            }
            int currentDeduct = Math.min(currentRemaining, remainingToDeduct);
            int nextRemaining = currentRemaining - currentDeduct;
            int nextUsed = safeInt(card.getUsedTimes()) + currentDeduct;
            int updated = userCardMapper.update(
                null,
                new LambdaUpdateWrapper<UserCard>()
                    .eq(UserCard::getId, card.getId())
                    .set(UserCard::getRemainingTimes, nextRemaining)
                    .set(UserCard::getUsedTimes, nextUsed)
                    .set(UserCard::getStatus, nextRemaining <= 0 ? "used_up" : "active")
                    .set(UserCard::getRemark, appendRemark(card.getRemark(), "人工扣次：" + remark))
            );
            if (updated <= 0) {
                throw new IllegalStateException("card changed while applying operation");
            }
            insertCardUsageRecord(card.getId(), userId, storeId, currentDeduct, operationNo, "人工扣次：" + remark);
            touchedCardId = card.getId();
            remainingToDeduct -= currentDeduct;
        }
        return touchedCardId;
    }

    private void insertWalletTransaction(
        Long userId,
        Long storeId,
        String bizType,
        String relatedAction,
        String amountType,
        String changeType,
        BigDecimal amount,
        String operationNo,
        String remark
    ) {
        UserStoreWallet wallet = findWallet(userId, storeId, false);
        BigDecimal balanceAfter = "principal".equals(amountType) ? resolveAvailablePrincipal(wallet) : resolveAvailableGift(wallet);
        BigDecimal balanceBefore = CHANGE_IN.equals(changeType) ? balanceAfter.subtract(amount) : balanceAfter.add(amount);
        WalletTransaction transaction = new WalletTransaction();
        transaction.setTransactionNo("WT" + UUID.randomUUID().toString().replace("-", "").substring(0, 18));
        transaction.setUserId(userId);
        transaction.setStoreId(storeId);
        transaction.setBizType(bizType);
        transaction.setAmountType(amountType);
        transaction.setBalanceBucket("available");
        transaction.setChangeType(changeType);
        transaction.setAmount(amount);
        transaction.setRelatedAction(relatedAction);
        transaction.setBizActionNo(operationNo);
        transaction.setBalanceBefore(balanceBefore);
        transaction.setBalanceAfter(balanceAfter);
        transaction.setRemark(truncate(remark, 240));
        walletTransactionMapper.insert(transaction);
    }

    private void insertCardUsageRecord(Long cardId, Long userId, Long storeId, int usedTimes, String operationNo, String remark) {
        CardUsageRecord record = new CardUsageRecord();
        record.setUsageNo("CU" + UUID.randomUUID().toString().replace("-", "").substring(0, 18));
        record.setUserCardId(cardId);
        record.setUserId(userId);
        record.setStoreId(storeId);
        record.setOrderNo(operationNo);
        record.setUsedTimes(usedTimes);
        record.setUsageTime(LocalDateTime.now());
        record.setOperatorType("mini_admin");
        record.setRemark(truncate(remark, 240));
        cardUsageRecordMapper.insert(record);
    }

    private UserCard createManualCard(Long userId, Long storeId, int deltaTimes, String operationNo, String remark) {
        LocalDateTime now = LocalDateTime.now();
        UserCard card = new UserCard();
        card.setUserId(userId);
        card.setStoreId(storeId);
        card.setCardProductId(0L);
        card.setCardType("manual");
        card.setSourceChannel("mini_admin");
        card.setCardNo("MANUAL" + UUID.randomUUID().toString().replace("-", "").substring(0, 18));
        card.setTotalTimes(deltaTimes);
        card.setUsedTimes(0);
        card.setRemainingTimes(deltaTimes);
        card.setPurchaseTime(now);
        card.setEffectiveTime(now);
        card.setStatus("active");
        card.setExternalOrderNo(operationNo);
        card.setRemark(truncate("人工加次：" + remark, 240));
        userCardMapper.insert(card);
        return card;
    }

    private void insertAssetOperation(
        String operationNo,
        String operationType,
        String changeType,
        MiniAdminSessionContext context,
        Long userId,
        Long storeId,
        Long walletId,
        Long userCardId,
        String amountType,
        BigDecimal principalAmount,
        BigDecimal giftAmount,
        int cardDeltaTimes,
        String remark
    ) {
        MiniAdminAssetOperation operation = new MiniAdminAssetOperation();
        operation.setOperationNo(operationNo);
        operation.setOperationType(operationType);
        operation.setChangeType(changeType);
        operation.setUserId(userId);
        operation.setStoreId(storeId);
        operation.setWalletId(walletId);
        operation.setUserCardId(userCardId);
        operation.setAmountType(amountType);
        operation.setPrincipalAmount(principalAmount);
        operation.setGiftAmount(giftAmount);
        operation.setTotalAmount(principalAmount.add(giftAmount));
        operation.setCardDeltaTimes(cardDeltaTimes);
        operation.setOperatorStaffId(context.getStaff().getId());
        operation.setOperatorRoleCode(context.getStaff().getRoleCode());
        operation.setRemark(truncate(remark, 240));
        assetOperationMapper.insert(operation);
    }

    private List<UserCard> loadAvailableCardsForUpdate(Long userId, Long storeId) {
        LocalDateTime now = LocalDateTime.now();
        return userCardMapper.selectList(
            new LambdaQueryWrapper<UserCard>()
                .eq(UserCard::getUserId, userId)
                .eq(UserCard::getStoreId, storeId)
                .eq(UserCard::getStatus, "active")
                .gt(UserCard::getRemainingTimes, 0)
                .and(w -> w.isNull(UserCard::getEffectiveTime).or().le(UserCard::getEffectiveTime, now))
                .and(w -> w.isNull(UserCard::getExpireTime).or().gt(UserCard::getExpireTime, now))
                .orderByAsc(UserCard::getExpireTime)
                .orderByAsc(UserCard::getId)
                .last("for update")
        );
    }

    private List<UserCard> loadCards(Long userId, Long storeId) {
        return userCardMapper.selectList(
            new LambdaQueryWrapper<UserCard>()
                .eq(UserCard::getUserId, userId)
                .eq(UserCard::getStoreId, storeId)
                .orderByDesc(UserCard::getId)
        );
    }

    private UserCard requireUserCard(Long cardId, Long userId, Long storeId, boolean forUpdate) {
        if (cardId == null) {
            throw new IllegalArgumentException("userCardId is required");
        }
        UserCard card = userCardMapper.selectOne(
            new LambdaQueryWrapper<UserCard>()
                .eq(UserCard::getId, cardId)
                .eq(UserCard::getUserId, userId)
                .eq(UserCard::getStoreId, storeId)
                .last(forUpdate ? "limit 1 for update" : "limit 1")
        );
        if (card == null) {
            throw new IllegalArgumentException("card not found");
        }
        return card;
    }

    private UserStoreWallet findWallet(Long userId, Long storeId, boolean createIfAbsent) {
        UserStoreWallet wallet = userStoreWalletMapper.selectOne(
            new LambdaQueryWrapper<UserStoreWallet>()
                .eq(UserStoreWallet::getUserId, userId)
                .eq(UserStoreWallet::getStoreId, storeId)
                .last(createIfAbsent ? "limit 1 for update" : "limit 1")
        );
        if (wallet == null && createIfAbsent) {
            wallet = buildEmptyWallet(userId, storeId);
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
        }
        return wallet;
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

    private Map<Long, WalletTotals> buildWalletTotals(List<Long> userIds, List<Long> storeIds) {
        if (userIds.isEmpty() || storeIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, WalletTotals> result = new LinkedHashMap<>();
        for (UserStoreWallet wallet : userStoreWalletMapper.selectList(
            new LambdaQueryWrapper<UserStoreWallet>()
                .in(UserStoreWallet::getUserId, userIds)
                .in(UserStoreWallet::getStoreId, storeIds)
        )) {
            WalletTotals current = result.getOrDefault(wallet.getUserId(), WalletTotals.ZERO);
            result.put(wallet.getUserId(), new WalletTotals(
                current.principal().add(resolveAvailablePrincipal(wallet)),
                current.gift().add(resolveAvailableGift(wallet))
            ));
        }
        return result;
    }

    private Map<Long, Integer> buildCardTotals(List<Long> userIds, List<Long> storeIds) {
        if (userIds.isEmpty() || storeIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, Integer> result = new LinkedHashMap<>();
        for (UserCard card : userCardMapper.selectList(
            new LambdaQueryWrapper<UserCard>()
                .in(UserCard::getUserId, userIds)
                .in(UserCard::getStoreId, storeIds)
                .eq(UserCard::getStatus, "active")
        )) {
            result.merge(card.getUserId(), Math.max(safeInt(card.getRemainingTimes()), 0), Integer::sum);
        }
        return result;
    }

    private Store requireAccessibleStore(MiniAdminSessionContext context, Long storeId) {
        if (storeId == null) {
            throw new IllegalArgumentException("storeId is required");
        }
        if (!miniAdminAuthService.canAccessStore(context, storeId)) {
            throw new IllegalArgumentException("无权操作该门店");
        }
        Store store = storeMapper.selectById(storeId);
        if (store == null) {
            throw new IllegalArgumentException("store not found");
        }
        return store;
    }

    private List<Long> resolveAccessibleStoreIds(MiniAdminSessionContext context, Long requestedStoreId) {
        if (requestedStoreId != null && requestedStoreId > 0) {
            requireAccessibleStore(context, requestedStoreId);
            return List.of(requestedStoreId);
        }
        return context.getStores().stream()
            .map(MiniAdminStoreOption::getId)
            .filter(id -> id != null && id > 0)
            .distinct()
            .toList();
    }

    private UserInfo requireUser(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId is required");
        }
        UserInfo user = userInfoMapper.selectById(userId);
        if (user == null) {
            throw new IllegalArgumentException("user not found");
        }
        return user;
    }

    private void requireAssetPermission(MiniAdminSessionContext context) {
        if (context == null) {
            throw new IllegalArgumentException("管理端登录已失效，请重新登录");
        }
        if (!miniAdminAuthService.hasPermission(context, PERMISSION_WALLET_ADJUST)
            && !miniAdminAuthService.hasPermission(context, PERMISSION_CARD_ADJUST)
            && !miniAdminAuthService.hasPermission(context, "user:view")) {
            throw new IllegalArgumentException("无用户资产权限");
        }
    }

    private void ensurePermission(MiniAdminSessionContext context, String permission) {
        if (context == null || !miniAdminAuthService.hasPermission(context, permission)) {
            throw new IllegalArgumentException("无操作权限");
        }
    }

    private MiniAdminUserCardItem toCardItem(UserCard card, Store store) {
        return new MiniAdminUserCardItem(
            card.getId(),
            card.getStoreId(),
            store != null ? store.getStoreName() : "",
            card.getCardNo(),
            card.getCardType(),
            card.getTotalTimes(),
            card.getUsedTimes(),
            card.getRemainingTimes(),
            card.getStatus(),
            card.getExpireTime(),
            card.getRemark()
        );
    }

    private MiniAdminAssetOperationResult buildResult(
        String operationNo,
        String operationType,
        Long userId,
        Long storeId,
        Long walletId,
        Long userCardId,
        BigDecimal principalAmount,
        BigDecimal giftAmount,
        int cardDeltaTimes,
        String remark
    ) {
        return new MiniAdminAssetOperationResult(
            operationNo,
            operationType,
            userId,
            storeId,
            walletId,
            userCardId,
            principalAmount,
            giftAmount,
            principalAmount.add(giftAmount),
            cardDeltaTimes,
            remark
        );
    }

    private String normalizeChangeType(String changeType) {
        String value = StringUtils.hasText(changeType) ? changeType.trim().toLowerCase() : "";
        if (!CHANGE_IN.equals(value) && !CHANGE_OUT.equals(value)) {
            throw new IllegalArgumentException("changeType must be in or out");
        }
        return value;
    }

    private BigDecimal normalizePositiveAmount(BigDecimal amount) {
        BigDecimal value = amount != null ? amount : BigDecimal.ZERO;
        value = value.setScale(2, RoundingMode.HALF_UP);
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("amount must be >= 0");
        }
        return value;
    }

    private BigDecimal resolveAmount(BigDecimal amount) {
        return amount != null ? amount : BigDecimal.ZERO;
    }

    private BigDecimal resolvePrincipalBalance(UserStoreWallet wallet) {
        return wallet != null ? resolveAmount(wallet.getPrincipalBalance()) : BigDecimal.ZERO;
    }

    private BigDecimal resolveGiftBalance(UserStoreWallet wallet) {
        return wallet != null ? resolveAmount(wallet.getGiftBalance()) : BigDecimal.ZERO;
    }

    private BigDecimal resolveAvailablePrincipal(UserStoreWallet wallet) {
        if (wallet == null) {
            return BigDecimal.ZERO;
        }
        return wallet.getAvailablePrincipalBalance() != null
            ? wallet.getAvailablePrincipalBalance()
            : resolvePrincipalBalance(wallet);
    }

    private BigDecimal resolveAvailableGift(UserStoreWallet wallet) {
        if (wallet == null) {
            return BigDecimal.ZERO;
        }
        return wallet.getAvailableGiftBalance() != null
            ? wallet.getAvailableGiftBalance()
            : resolveGiftBalance(wallet);
    }

    private String requireRemark(String remark) {
        if (!StringUtils.hasText(remark)) {
            throw new IllegalArgumentException("remark is required");
        }
        return truncate(remark.trim(), 240);
    }

    private String resolveAmountType(BigDecimal principalAmount, BigDecimal giftAmount) {
        if (principalAmount.compareTo(BigDecimal.ZERO) > 0 && giftAmount.compareTo(BigDecimal.ZERO) > 0) {
            return "mixed";
        }
        return principalAmount.compareTo(BigDecimal.ZERO) > 0 ? "principal" : "gift";
    }

    private String buildWalletAdjustRemark(String changeType, String label, String remark) {
        return (CHANGE_IN.equals(changeType) ? "人工加款：" : "人工扣款：") + label + "，" + remark;
    }

    private String buildOperationNo(String prefix) {
        return prefix + UUID.randomUUID().toString().replace("-", "").substring(0, 18);
    }

    private int safeInt(Integer value) {
        return value != null ? value : 0;
    }

    private String appendRemark(String oldRemark, String addition) {
        String next = StringUtils.hasText(oldRemark) ? oldRemark.trim() + "；" + addition : addition;
        return truncate(next, 240);
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private Long parseLong(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private record WalletTotals(BigDecimal principal, BigDecimal gift) {
        private static final WalletTotals ZERO = new WalletTotals(BigDecimal.ZERO, BigDecimal.ZERO);
    }
}
