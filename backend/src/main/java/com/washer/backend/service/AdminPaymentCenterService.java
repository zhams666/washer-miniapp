package com.washer.backend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.washer.backend.dto.admin.AdminCardUsageCenterItem;
import com.washer.backend.dto.admin.AdminPaymentDetailItem;
import com.washer.backend.dto.admin.AdminWalletTransactionCenterItem;

public interface AdminPaymentCenterService {

    Page<AdminPaymentDetailItem> pagePaymentDetails(
        long page,
        long size,
        String orderNo,
        Long userId,
        Long storeId,
        String payMode,
        String paymentStatus
    );

    Page<AdminWalletTransactionCenterItem> pageWalletTransactions(
        long page,
        long size,
        Long userId,
        Long storeId,
        String bizType,
        String relatedOrderNo
    );

    Page<AdminCardUsageCenterItem> pageCardUsages(
        long page,
        long size,
        Long userId,
        Long storeId,
        String cardNo,
        String orderNo
    );
}
