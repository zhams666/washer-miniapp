package com.washer.backend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.washer.backend.dto.admin.AdminCardUsageCenterItem;
import com.washer.backend.dto.admin.AdminPaymentDetailItem;
import com.washer.backend.dto.admin.AdminSettlementBillGenerateRequest;
import com.washer.backend.dto.admin.AdminSettlementBillGenerateResult;
import com.washer.backend.dto.admin.AdminSettlementBillItem;
import com.washer.backend.dto.admin.AdminSettlementDetailItem;
import com.washer.backend.dto.admin.AdminWalletTransactionCenterItem;
import java.time.LocalDate;

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

    Page<AdminSettlementDetailItem> pageSettlementDetails(
        long page,
        long size,
        Long fromStoreId,
        Long toStoreId,
        String orderNo,
        LocalDate bizDate,
        Long billId,
        String billNo
    );

    Page<AdminSettlementBillItem> pageSettlementBills(
        long page,
        long size,
        Long fromStoreId,
        Long toStoreId,
        String billNo,
        LocalDate startDate,
        LocalDate endDate,
        String settlementStatus
    );

    AdminSettlementBillGenerateResult generateSettlementBills(AdminSettlementBillGenerateRequest request);
}
