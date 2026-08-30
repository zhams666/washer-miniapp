package com.washer.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.washer.backend.dto.admin.AdminWalletRefundRequest;
import com.washer.backend.dto.admin.AdminWalletRefundResult;
import com.washer.backend.entity.Store;
import com.washer.backend.entity.UserInfo;
import com.washer.backend.entity.UserStoreWallet;
import com.washer.backend.entity.WalletTransaction;
import com.washer.backend.mapper.UserStoreWalletMapper;
import com.washer.backend.mapper.WalletTransactionMapper;
import com.washer.backend.service.AdminWalletRefundService;
import com.washer.backend.service.StoreService;
import com.washer.backend.service.UserInfoService;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AdminWalletRefundServiceImpl implements AdminWalletRefundService {

    private final UserInfoService userInfoService;
    private final StoreService storeService;
    private final UserStoreWalletMapper userStoreWalletMapper;
    private final WalletTransactionMapper walletTransactionMapper;

    public AdminWalletRefundServiceImpl(
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
    public AdminWalletRefundResult manualRefund(AdminWalletRefundRequest request) {
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
        if (principalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("principalAmount must be > 0");
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
                .last("limit 1")
        );
        if (wallet == null) {
            throw new IllegalArgumentException("wallet not found");
        }

        BigDecimal principalAvailableBefore = resolveAvailablePrincipal(wallet);
        if (principalAvailableBefore.compareTo(principalAmount) < 0) {
            throw new IllegalArgumentException("wallet principal balance is not enough");
        }

        BigDecimal principalBalanceBefore = resolvePrincipalBalance(wallet);
        BigDecimal giftBalanceBefore = resolveGiftBalance(wallet);
        BigDecimal giftAvailableBefore = resolveAvailableGift(wallet);
        BigDecimal principalAvailableAfter = principalAvailableBefore.subtract(principalAmount);
        BigDecimal principalBalanceAfter = principalBalanceBefore.subtract(principalAmount);
        BigDecimal totalRefundPrincipal = resolveAmount(wallet.getTotalRefundPrincipal()).add(principalAmount);

        LambdaUpdateWrapper<UserStoreWallet> walletWrapper = new LambdaUpdateWrapper<UserStoreWallet>()
            .eq(UserStoreWallet::getId, wallet.getId())
            .set(UserStoreWallet::getPrincipalBalance, principalBalanceAfter)
            .set(UserStoreWallet::getAvailablePrincipalBalance, principalAvailableAfter)
            .set(UserStoreWallet::getTotalRefundPrincipal, totalRefundPrincipal);

        if (giftAvailableBefore.compareTo(BigDecimal.ZERO) > 0 || giftBalanceBefore.compareTo(BigDecimal.ZERO) > 0) {
            walletWrapper
                .set(UserStoreWallet::getGiftBalance, BigDecimal.ZERO)
                .set(UserStoreWallet::getAvailableGiftBalance, BigDecimal.ZERO)
                .set(UserStoreWallet::getFrozenGiftBalance, BigDecimal.ZERO);
        }
        userStoreWalletMapper.update(null, walletWrapper);

        String bizActionNo = "MANUAL_REFUND_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        insertWalletTransaction(
            request.getUserId(),
            request.getStoreId(),
            principalAvailableBefore,
            principalAvailableAfter,
            principalAmount,
            bizActionNo,
            request.getRemark()
        );
        if (giftAvailableBefore.compareTo(BigDecimal.ZERO) > 0) {
            insertGiftClearTransaction(
                request.getUserId(),
                request.getStoreId(),
                giftAvailableBefore,
                BigDecimal.ZERO,
                giftAvailableBefore,
                bizActionNo,
                request.getRemark()
            );
        }

        return new AdminWalletRefundResult(wallet.getId(), request.getUserId(), request.getStoreId(), principalAmount);
    }

    private void insertWalletTransaction(
        Long userId,
        Long storeId,
        BigDecimal balanceBefore,
        BigDecimal balanceAfter,
        BigDecimal amount,
        String bizActionNo,
        String remark
    ) {
        WalletTransaction transaction = new WalletTransaction();
        transaction.setTransactionNo("WT" + UUID.randomUUID().toString().replace("-", "").substring(0, 18));
        transaction.setUserId(userId);
        transaction.setStoreId(storeId);
        transaction.setBizType("refund");
        transaction.setAmountType("principal");
        transaction.setBalanceBucket("available");
        transaction.setChangeType("out");
        transaction.setAmount(amount);
        transaction.setRelatedAction("refund");
        transaction.setBizActionNo(bizActionNo);
        transaction.setBalanceBefore(balanceBefore);
        transaction.setBalanceAfter(balanceAfter);
        transaction.setRemark(buildRefundRemark(remark));
        walletTransactionMapper.insert(transaction);
    }

    private void insertGiftClearTransaction(
        Long userId,
        Long storeId,
        BigDecimal balanceBefore,
        BigDecimal balanceAfter,
        BigDecimal amount,
        String bizActionNo,
        String remark
    ) {
        WalletTransaction transaction = new WalletTransaction();
        transaction.setTransactionNo("WT" + UUID.randomUUID().toString().replace("-", "").substring(0, 18));
        transaction.setUserId(userId);
        transaction.setStoreId(storeId);
        transaction.setBizType("refund");
        transaction.setAmountType("gift");
        transaction.setBalanceBucket("available");
        transaction.setChangeType("out");
        transaction.setAmount(amount);
        transaction.setRelatedAction("refund");
        transaction.setBizActionNo(bizActionNo);
        transaction.setBalanceBefore(balanceBefore);
        transaction.setBalanceAfter(balanceAfter);
        transaction.setRemark(buildGiftClearRemark(remark));
        walletTransactionMapper.insert(transaction);
    }

    private String buildRefundRemark(String remark) {
        if (StringUtils.hasText(remark)) {
            return "manual principal refund: " + remark;
        }
        return "manual principal refund";
    }

    private String buildGiftClearRemark(String remark) {
        if (StringUtils.hasText(remark)) {
            return "clear gift balance with refund: " + remark;
        }
        return "clear gift balance with refund";
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
}
