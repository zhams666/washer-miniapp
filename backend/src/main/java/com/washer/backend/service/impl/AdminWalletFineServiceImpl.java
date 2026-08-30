package com.washer.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.washer.backend.dto.admin.AdminWalletFineRequest;
import com.washer.backend.dto.admin.AdminWalletFineResult;
import com.washer.backend.entity.Store;
import com.washer.backend.entity.UserInfo;
import com.washer.backend.entity.UserStoreWallet;
import com.washer.backend.entity.WalletTransaction;
import com.washer.backend.mapper.UserStoreWalletMapper;
import com.washer.backend.mapper.WalletTransactionMapper;
import com.washer.backend.service.AdminWalletFineService;
import com.washer.backend.service.StoreService;
import com.washer.backend.service.UserInfoService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AdminWalletFineServiceImpl implements AdminWalletFineService {

    private static final String BIZ_TYPE_FINE = "fine";
    private static final String RELATED_ACTION_FINE = "fine";

    private final UserInfoService userInfoService;
    private final StoreService storeService;
    private final UserStoreWalletMapper userStoreWalletMapper;
    private final WalletTransactionMapper walletTransactionMapper;

    public AdminWalletFineServiceImpl(
        UserInfoService userInfoService,
        StoreService storeService,
        UserStoreWalletMapper userStoreWalletMapper,
        WalletTransactionMapper walletTransactionMapper
    ) {
        this.userInfoService = userInfoService;
        this.storeService = storeService;
        this.userStoreWalletMapper = userStoreWalletMapper;
        this.walletTransactionMapper = walletTransactionMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminWalletFineResult manualFine(AdminWalletFineRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request is required");
        }
        if (request.getUserId() == null) {
            throw new IllegalArgumentException("userId is required");
        }
        if (request.getStoreId() == null) {
            throw new IllegalArgumentException("storeId is required");
        }

        BigDecimal fineAmount = normalizeAmount(request.getAmount());
        if (fineAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amount must be > 0");
        }

        UserInfo user = userInfoService.getById(request.getUserId());
        if (user == null) {
            throw new IllegalArgumentException("user not found");
        }
        Store store = storeService.getById(request.getStoreId());
        if (store == null) {
            throw new IllegalArgumentException("store not found");
        }

        UserStoreWallet wallet = userStoreWalletMapper.selectOne(
            new LambdaQueryWrapper<UserStoreWallet>()
                .eq(UserStoreWallet::getUserId, request.getUserId())
                .eq(UserStoreWallet::getStoreId, request.getStoreId())
                .last("limit 1 for update")
        );
        if (wallet == null) {
            throw new IllegalArgumentException("wallet not found");
        }

        BigDecimal availablePrincipalBefore = nonNegative(resolveAvailablePrincipal(wallet));
        BigDecimal availableGiftBefore = nonNegative(resolveAvailableGift(wallet));
        BigDecimal availableTotal = availablePrincipalBefore.add(availableGiftBefore);
        if (availableTotal.compareTo(fineAmount) < 0) {
            throw new IllegalArgumentException("wallet balance is not enough");
        }

        BigDecimal principalFineAmount = fineAmount.min(availablePrincipalBefore);
        BigDecimal giftFineAmount = fineAmount.subtract(principalFineAmount);

        BigDecimal principalBalanceBefore = resolvePrincipalBalance(wallet);
        BigDecimal giftBalanceBefore = resolveGiftBalance(wallet);
        BigDecimal principalBalanceAfter = principalBalanceBefore.subtract(principalFineAmount);
        BigDecimal availablePrincipalAfter = availablePrincipalBefore.subtract(principalFineAmount);
        BigDecimal giftBalanceAfter = giftBalanceBefore.subtract(giftFineAmount);
        BigDecimal availableGiftAfter = availableGiftBefore.subtract(giftFineAmount);
        BigDecimal totalConsumePrincipal = resolveAmount(wallet.getTotalConsumePrincipal()).add(principalFineAmount);
        BigDecimal totalConsumeGift = resolveAmount(wallet.getTotalConsumeGift()).add(giftFineAmount);

        int updatedRows = userStoreWalletMapper.update(
            null,
            new LambdaUpdateWrapper<UserStoreWallet>()
                .eq(UserStoreWallet::getId, wallet.getId())
                .set(UserStoreWallet::getPrincipalBalance, principalBalanceAfter)
                .set(UserStoreWallet::getAvailablePrincipalBalance, availablePrincipalAfter)
                .set(UserStoreWallet::getGiftBalance, giftBalanceAfter)
                .set(UserStoreWallet::getAvailableGiftBalance, availableGiftAfter)
                .set(UserStoreWallet::getTotalConsumePrincipal, totalConsumePrincipal)
                .set(UserStoreWallet::getTotalConsumeGift, totalConsumeGift)
        );
        if (updatedRows <= 0) {
            throw new IllegalStateException("wallet changed while applying fine");
        }

        String bizActionNo = "MANUAL_FINE_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        List<String> transactionNos = new ArrayList<>();
        if (principalFineAmount.compareTo(BigDecimal.ZERO) > 0) {
            transactionNos.add(insertFineTransaction(
                request.getUserId(),
                request.getStoreId(),
                "principal",
                availablePrincipalBefore,
                availablePrincipalAfter,
                principalFineAmount,
                bizActionNo,
                request.getRemark()
            ));
        }
        if (giftFineAmount.compareTo(BigDecimal.ZERO) > 0) {
            transactionNos.add(insertFineTransaction(
                request.getUserId(),
                request.getStoreId(),
                "gift",
                availableGiftBefore,
                availableGiftAfter,
                giftFineAmount,
                bizActionNo,
                request.getRemark()
            ));
        }

        return new AdminWalletFineResult(
            wallet.getId(),
            request.getUserId(),
            request.getStoreId(),
            fineAmount,
            principalFineAmount,
            giftFineAmount,
            bizActionNo,
            transactionNos
        );
    }

    private String insertFineTransaction(
        Long userId,
        Long storeId,
        String amountType,
        BigDecimal balanceBefore,
        BigDecimal balanceAfter,
        BigDecimal amount,
        String bizActionNo,
        String remark
    ) {
        String transactionNo = "WT" + UUID.randomUUID().toString().replace("-", "").substring(0, 18);
        WalletTransaction transaction = new WalletTransaction();
        transaction.setTransactionNo(transactionNo);
        transaction.setUserId(userId);
        transaction.setStoreId(storeId);
        transaction.setBizType(BIZ_TYPE_FINE);
        transaction.setAmountType(amountType);
        transaction.setBalanceBucket("available");
        transaction.setChangeType("out");
        transaction.setAmount(amount);
        transaction.setRelatedAction(RELATED_ACTION_FINE);
        transaction.setBizActionNo(bizActionNo);
        transaction.setBalanceBefore(balanceBefore);
        transaction.setBalanceAfter(balanceAfter);
        transaction.setRemark(buildFineRemark(remark));
        walletTransactionMapper.insert(transaction);
        return transactionNo;
    }

    private String buildFineRemark(String remark) {
        String prefix = "后台罚款";
        if (!StringUtils.hasText(remark)) {
            return prefix;
        }
        String value = remark.trim();
        int maxRemarkLength = 240 - prefix.length();
        if (value.length() > maxRemarkLength) {
            value = value.substring(0, maxRemarkLength);
        }
        return prefix + "：" + value;
    }

    private BigDecimal normalizeAmount(BigDecimal amount) {
        return amount != null ? amount : BigDecimal.ZERO;
    }

    private BigDecimal resolveAmount(BigDecimal amount) {
        return amount != null ? amount : BigDecimal.ZERO;
    }

    private BigDecimal nonNegative(BigDecimal amount) {
        BigDecimal safeAmount = resolveAmount(amount);
        return safeAmount.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : safeAmount;
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
}
