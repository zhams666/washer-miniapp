package com.washer.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.washer.backend.dto.admin.AdminCardUsageCenterItem;
import com.washer.backend.dto.admin.AdminPaymentDetailItem;
import com.washer.backend.dto.admin.AdminWalletTransactionCenterItem;
import com.washer.backend.entity.CardUsageRecord;
import com.washer.backend.entity.Store;
import com.washer.backend.entity.UserCard;
import com.washer.backend.entity.WalletTransaction;
import com.washer.backend.entity.WashOrder;
import com.washer.backend.entity.WashOrderPaymentDetail;
import com.washer.backend.mapper.CardUsageRecordMapper;
import com.washer.backend.mapper.UserCardMapper;
import com.washer.backend.mapper.WalletTransactionMapper;
import com.washer.backend.mapper.WashOrderMapper;
import com.washer.backend.mapper.WashOrderPaymentDetailMapper;
import com.washer.backend.service.AdminPaymentCenterService;
import com.washer.backend.service.StoreService;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AdminPaymentCenterServiceImpl implements AdminPaymentCenterService {

    private final StoreService storeService;
    private final WashOrderMapper washOrderMapper;
    private final UserCardMapper userCardMapper;
    private final WashOrderPaymentDetailMapper washOrderPaymentDetailMapper;
    private final WalletTransactionMapper walletTransactionMapper;
    private final CardUsageRecordMapper cardUsageRecordMapper;

    public AdminPaymentCenterServiceImpl(
        StoreService storeService,
        WashOrderMapper washOrderMapper,
        UserCardMapper userCardMapper,
        WashOrderPaymentDetailMapper washOrderPaymentDetailMapper,
        WalletTransactionMapper walletTransactionMapper,
        CardUsageRecordMapper cardUsageRecordMapper
    ) {
        this.storeService = storeService;
        this.washOrderMapper = washOrderMapper;
        this.userCardMapper = userCardMapper;
        this.washOrderPaymentDetailMapper = washOrderPaymentDetailMapper;
        this.walletTransactionMapper = walletTransactionMapper;
        this.cardUsageRecordMapper = cardUsageRecordMapper;
    }

    @Override
    public Page<AdminPaymentDetailItem> pagePaymentDetails(
        long page,
        long size,
        String orderNo,
        Long userId,
        Long storeId,
        String payMode,
        String paymentStatus
    ) {
        LambdaQueryWrapper<WashOrderPaymentDetail> wrapper = new LambdaQueryWrapper<WashOrderPaymentDetail>()
            .eq(userId != null, WashOrderPaymentDetail::getUserId, userId)
            .eq(storeId != null, WashOrderPaymentDetail::getConsumeStoreId, storeId)
            .eq(StringUtils.hasText(payMode), WashOrderPaymentDetail::getSourceType, payMode)
            .like(StringUtils.hasText(orderNo), WashOrderPaymentDetail::getOrderNo, orderNo)
            .orderByDesc(WashOrderPaymentDetail::getId);

        if (StringUtils.hasText(paymentStatus)) {
            List<Long> matchedOrderIds = washOrderMapper.selectList(
                new LambdaQueryWrapper<WashOrder>()
                    .eq(WashOrder::getPaymentStatus, paymentStatus)
                    .select(WashOrder::getId)
            ).stream().map(WashOrder::getId).toList();

            if (matchedOrderIds.isEmpty()) {
                return new Page<>(page, size, 0);
            }

            wrapper.in(WashOrderPaymentDetail::getOrderId, matchedOrderIds);
        }

        Page<WashOrderPaymentDetail> detailPage = washOrderPaymentDetailMapper.selectPage(new Page<>(page, size), wrapper);
        Map<Long, Store> storeMap = buildStoreMapFromPaymentDetails(detailPage.getRecords());
        Map<Long, WashOrder> orderMap = buildOrderMap(detailPage.getRecords().stream().map(WashOrderPaymentDetail::getOrderId).toList());
        Map<Long, UserCard> userCardMap = buildUserCardMap(detailPage.getRecords().stream().map(WashOrderPaymentDetail::getUserCardId).toList());

        Page<AdminPaymentDetailItem> result = new Page<>(detailPage.getCurrent(), detailPage.getSize(), detailPage.getTotal());
        result.setRecords(detailPage.getRecords().stream()
            .map(detail -> toPaymentDetailItem(detail, storeMap, orderMap, userCardMap))
            .toList());
        return result;
    }

    @Override
    public Page<AdminWalletTransactionCenterItem> pageWalletTransactions(
        long page,
        long size,
        Long userId,
        Long storeId,
        String bizType,
        String relatedOrderNo
    ) {
        LambdaQueryWrapper<WalletTransaction> wrapper = new LambdaQueryWrapper<WalletTransaction>()
            .eq(userId != null, WalletTransaction::getUserId, userId)
            .eq(storeId != null, WalletTransaction::getStoreId, storeId)
            .eq(StringUtils.hasText(bizType), WalletTransaction::getBizType, bizType)
            .like(StringUtils.hasText(relatedOrderNo), WalletTransaction::getRelatedOrderNo, relatedOrderNo)
            .orderByDesc(WalletTransaction::getId);

        Page<WalletTransaction> txPage = walletTransactionMapper.selectPage(new Page<>(page, size), wrapper);
        Map<Long, Store> storeMap = buildStoreMap(txPage.getRecords().stream().map(WalletTransaction::getStoreId).toList());

        Page<AdminWalletTransactionCenterItem> result = new Page<>(txPage.getCurrent(), txPage.getSize(), txPage.getTotal());
        result.setRecords(txPage.getRecords().stream()
            .map(tx -> new AdminWalletTransactionCenterItem(
                tx.getId(),
                tx.getTransactionNo(),
                tx.getUserId(),
                tx.getStoreId(),
                resolveStoreName(storeMap, tx.getStoreId()),
                tx.getBizType(),
                tx.getAmountType(),
                tx.getChangeType(),
                normalizeAmount(tx.getAmount()),
                normalizeAmount(tx.getBalanceAfter()),
                tx.getRelatedOrderId(),
                tx.getRelatedOrderNo(),
                tx.getRemark(),
                tx.getCreatedAt()
            ))
            .toList());
        return result;
    }

    @Override
    public Page<AdminCardUsageCenterItem> pageCardUsages(
        long page,
        long size,
        Long userId,
        Long storeId,
        String cardNo,
        String orderNo
    ) {
        LambdaQueryWrapper<CardUsageRecord> wrapper = new LambdaQueryWrapper<CardUsageRecord>()
            .eq(userId != null, CardUsageRecord::getUserId, userId)
            .eq(storeId != null, CardUsageRecord::getStoreId, storeId)
            .like(StringUtils.hasText(orderNo), CardUsageRecord::getOrderNo, orderNo)
            .orderByDesc(CardUsageRecord::getId);

        if (StringUtils.hasText(cardNo)) {
            List<Long> matchedCardIds = userCardMapper.selectList(
                new LambdaQueryWrapper<UserCard>()
                    .like(UserCard::getCardNo, cardNo)
                    .select(UserCard::getId)
            ).stream().map(UserCard::getId).toList();

            if (matchedCardIds.isEmpty()) {
                return new Page<>(page, size, 0);
            }

            wrapper.in(CardUsageRecord::getUserCardId, matchedCardIds);
        }

        Page<CardUsageRecord> usagePage = cardUsageRecordMapper.selectPage(new Page<>(page, size), wrapper);
        Map<Long, Store> storeMap = buildStoreMap(usagePage.getRecords().stream().map(CardUsageRecord::getStoreId).toList());
        Map<Long, UserCard> userCardMap = buildUserCardMap(usagePage.getRecords().stream().map(CardUsageRecord::getUserCardId).toList());

        Page<AdminCardUsageCenterItem> result = new Page<>(usagePage.getCurrent(), usagePage.getSize(), usagePage.getTotal());
        result.setRecords(usagePage.getRecords().stream()
            .map(record -> new AdminCardUsageCenterItem(
                record.getId(),
                record.getUsageNo(),
                record.getUserCardId(),
                resolveCardNo(userCardMap, record.getUserCardId()),
                record.getUserId(),
                record.getStoreId(),
                resolveStoreName(storeMap, record.getStoreId()),
                record.getOrderId(),
                record.getOrderNo(),
                record.getUsedTimes(),
                record.getUsageTime(),
                record.getRemark(),
                record.getCreatedAt()
            ))
            .toList());
        return result;
    }

    private AdminPaymentDetailItem toPaymentDetailItem(
        WashOrderPaymentDetail detail,
        Map<Long, Store> storeMap,
        Map<Long, WashOrder> orderMap,
        Map<Long, UserCard> userCardMap
    ) {
        WashOrder order = detail.getOrderId() != null ? orderMap.get(detail.getOrderId()) : null;
        return new AdminPaymentDetailItem(
            detail.getId(),
            detail.getOrderId(),
            detail.getOrderNo(),
            detail.getUserId(),
            detail.getConsumeStoreId(),
            resolveStoreName(storeMap, detail.getConsumeStoreId()),
            detail.getSourceType(),
            order != null ? order.getPaymentStatus() : "",
            detail.getAmountType(),
            detail.getUserCardId(),
            resolveCardNo(userCardMap, detail.getUserCardId()),
            normalizeAmount(detail.getAmount()),
            detail.getDeductTimes(),
            detail.getPaymentSeq(),
            detail.getSettleStage(),
            detail.getAllocationStrategy(),
            normalizeAmount(detail.getRefundedAmount()),
            detail.getCreatedAt()
        );
    }

    private Map<Long, Store> buildStoreMapFromPaymentDetails(List<WashOrderPaymentDetail> details) {
        return buildStoreMap(details.stream().map(WashOrderPaymentDetail::getConsumeStoreId).toList());
    }

    private Map<Long, Store> buildStoreMap(List<Long> storeIds) {
        List<Long> filteredStoreIds = storeIds.stream()
            .filter(storeId -> storeId != null)
            .distinct()
            .toList();

        if (filteredStoreIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return storeService.listByIds(filteredStoreIds).stream()
            .collect(Collectors.toMap(Store::getId, Function.identity(), (left, right) -> left));
    }

    private Map<Long, WashOrder> buildOrderMap(List<Long> orderIds) {
        List<Long> filteredOrderIds = orderIds.stream()
            .filter(orderId -> orderId != null)
            .distinct()
            .toList();

        if (filteredOrderIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return washOrderMapper.selectBatchIds(filteredOrderIds).stream()
            .collect(Collectors.toMap(WashOrder::getId, Function.identity(), (left, right) -> left));
    }

    private Map<Long, UserCard> buildUserCardMap(List<Long> userCardIds) {
        List<Long> filteredUserCardIds = userCardIds.stream()
            .filter(userCardId -> userCardId != null)
            .distinct()
            .toList();

        if (filteredUserCardIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return userCardMapper.selectBatchIds(filteredUserCardIds).stream()
            .collect(Collectors.toMap(UserCard::getId, Function.identity(), (left, right) -> left));
    }

    private String resolveStoreName(Map<Long, Store> storeMap, Long storeId) {
        if (storeId == null) {
            return "";
        }
        Store store = storeMap.get(storeId);
        return store != null ? store.getStoreName() : "";
    }

    private String resolveCardNo(Map<Long, UserCard> userCardMap, Long userCardId) {
        if (userCardId == null) {
            return "";
        }
        UserCard userCard = userCardMap.get(userCardId);
        return userCard != null ? userCard.getCardNo() : "";
    }

    private BigDecimal normalizeAmount(BigDecimal amount) {
        return amount != null ? amount : BigDecimal.ZERO;
    }
}
