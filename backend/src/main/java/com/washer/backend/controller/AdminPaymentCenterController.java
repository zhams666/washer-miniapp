package com.washer.backend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.washer.backend.common.ApiResponse;
import com.washer.backend.dto.admin.AdminCardUsageCenterItem;
import com.washer.backend.dto.admin.AdminPaymentDetailItem;
import com.washer.backend.dto.admin.AdminSettlementBillGenerateRequest;
import com.washer.backend.dto.admin.AdminSettlementBillGenerateResult;
import com.washer.backend.dto.admin.AdminSettlementBillItem;
import com.washer.backend.dto.admin.AdminSettlementDetailItem;
import com.washer.backend.dto.admin.AdminWalletTransactionCenterItem;
import com.washer.backend.service.AdminPaymentCenterService;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminPaymentCenterController {

    private final AdminPaymentCenterService adminPaymentCenterService;

    public AdminPaymentCenterController(AdminPaymentCenterService adminPaymentCenterService) {
        this.adminPaymentCenterService = adminPaymentCenterService;
    }

    @GetMapping("/payment-details")
    public ApiResponse<Page<AdminPaymentDetailItem>> pagePaymentDetails(
        @RequestParam(defaultValue = "1") long page,
        @RequestParam(defaultValue = "10") long size,
        @RequestParam(required = false) String orderNo,
        @RequestParam(required = false) Long userId,
        @RequestParam(required = false) Long storeId,
        @RequestParam(required = false) String payMode,
        @RequestParam(required = false) String paymentStatus
    ) {
        return ApiResponse.success(
            adminPaymentCenterService.pagePaymentDetails(page, size, orderNo, userId, storeId, payMode, paymentStatus)
        );
    }

    @GetMapping("/wallet-transactions")
    public ApiResponse<Page<AdminWalletTransactionCenterItem>> pageWalletTransactions(
        @RequestParam(defaultValue = "1") long page,
        @RequestParam(defaultValue = "10") long size,
        @RequestParam(required = false) Long userId,
        @RequestParam(required = false) Long storeId,
        @RequestParam(required = false) String bizType,
        @RequestParam(required = false) String relatedOrderNo
    ) {
        return ApiResponse.success(
            adminPaymentCenterService.pageWalletTransactions(page, size, userId, storeId, bizType, relatedOrderNo)
        );
    }

    @GetMapping("/card-usages")
    public ApiResponse<Page<AdminCardUsageCenterItem>> pageCardUsages(
        @RequestParam(defaultValue = "1") long page,
        @RequestParam(defaultValue = "10") long size,
        @RequestParam(required = false) Long userId,
        @RequestParam(required = false) Long storeId,
        @RequestParam(required = false) String cardNo,
        @RequestParam(required = false) String orderNo
    ) {
        return ApiResponse.success(
            adminPaymentCenterService.pageCardUsages(page, size, userId, storeId, cardNo, orderNo)
        );
    }

    @GetMapping("/settlement-details")
    public ApiResponse<Page<AdminSettlementDetailItem>> pageSettlementDetails(
        @RequestParam(defaultValue = "1") long page,
        @RequestParam(defaultValue = "10") long size,
        @RequestParam(required = false) Long fromStoreId,
        @RequestParam(name = "from_store_id", required = false) Long fromStoreIdSnake,
        @RequestParam(required = false) Long toStoreId,
        @RequestParam(name = "to_store_id", required = false) Long toStoreIdSnake,
        @RequestParam(required = false) String orderNo,
        @RequestParam(name = "order_no", required = false) String orderNoSnake,
        @RequestParam(required = false) Long billId,
        @RequestParam(name = "bill_id", required = false) Long billIdSnake,
        @RequestParam(required = false) String billNo,
        @RequestParam(name = "bill_no", required = false) String billNoSnake,
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate bizDate,
        @RequestParam(name = "biz_date", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate bizDateSnake
    ) {
        Long resolvedFromStoreId = fromStoreId != null ? fromStoreId : fromStoreIdSnake;
        Long resolvedToStoreId = toStoreId != null ? toStoreId : toStoreIdSnake;
        String resolvedOrderNo = orderNo != null ? orderNo : orderNoSnake;
        Long resolvedBillId = billId != null ? billId : billIdSnake;
        String resolvedBillNo = billNo != null ? billNo : billNoSnake;
        LocalDate resolvedBizDate = bizDate != null ? bizDate : bizDateSnake;
        return ApiResponse.success(
            adminPaymentCenterService.pageSettlementDetails(
                page,
                size,
                resolvedFromStoreId,
                resolvedToStoreId,
                resolvedOrderNo,
                resolvedBizDate,
                resolvedBillId,
                resolvedBillNo
            )
        );
    }

    @GetMapping("/settlement-bills")
    public ApiResponse<Page<AdminSettlementBillItem>> pageSettlementBills(
        @RequestParam(defaultValue = "1") long page,
        @RequestParam(defaultValue = "10") long size,
        @RequestParam(required = false) Long fromStoreId,
        @RequestParam(name = "from_store_id", required = false) Long fromStoreIdSnake,
        @RequestParam(required = false) Long toStoreId,
        @RequestParam(name = "to_store_id", required = false) Long toStoreIdSnake,
        @RequestParam(required = false) String billNo,
        @RequestParam(name = "bill_no", required = false) String billNoSnake,
        @RequestParam(required = false) String settlementStatus,
        @RequestParam(name = "settlement_status", required = false) String settlementStatusSnake,
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
        @RequestParam(name = "start_date", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDateSnake,
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
        @RequestParam(name = "end_date", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDateSnake
    ) {
        Long resolvedFromStoreId = fromStoreId != null ? fromStoreId : fromStoreIdSnake;
        Long resolvedToStoreId = toStoreId != null ? toStoreId : toStoreIdSnake;
        String resolvedBillNo = billNo != null ? billNo : billNoSnake;
        String resolvedStatus = settlementStatus != null ? settlementStatus : settlementStatusSnake;
        LocalDate resolvedStartDate = startDate != null ? startDate : startDateSnake;
        LocalDate resolvedEndDate = endDate != null ? endDate : endDateSnake;
        return ApiResponse.success(
            adminPaymentCenterService.pageSettlementBills(
                page,
                size,
                resolvedFromStoreId,
                resolvedToStoreId,
                resolvedBillNo,
                resolvedStartDate,
                resolvedEndDate,
                resolvedStatus
            )
        );
    }

    @PostMapping("/settlement-bills/generate")
    public ApiResponse<AdminSettlementBillGenerateResult> generateSettlementBills(
        @RequestBody AdminSettlementBillGenerateRequest request
    ) {
        return ApiResponse.success(adminPaymentCenterService.generateSettlementBills(request));
    }
}
