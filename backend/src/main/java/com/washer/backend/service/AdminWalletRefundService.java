package com.washer.backend.service;

import com.washer.backend.dto.admin.AdminWalletRefundRequest;
import com.washer.backend.dto.admin.AdminWalletRefundResult;

public interface AdminWalletRefundService {

    AdminWalletRefundResult manualRefund(AdminWalletRefundRequest request);
}
