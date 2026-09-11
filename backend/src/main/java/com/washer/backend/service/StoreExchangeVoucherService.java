package com.washer.backend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.washer.backend.dto.miniadmin.MiniAdminExchangeVoucherBatchResult;
import com.washer.backend.dto.miniadmin.MiniAdminExchangeVoucherCreateRequest;
import com.washer.backend.dto.miniadmin.MiniAdminExchangeVoucherItem;
import com.washer.backend.dto.miniadmin.MiniAdminSessionContext;
import java.util.Map;

public interface StoreExchangeVoucherService {

    MiniAdminExchangeVoucherBatchResult createBatch(
        MiniAdminSessionContext context,
        MiniAdminExchangeVoucherCreateRequest request
    );

    Page<MiniAdminExchangeVoucherItem> pageVouchers(
        MiniAdminSessionContext context,
        long page,
        long size,
        Long storeId,
        String status,
        String keyword
    );

    Page<MiniAdminExchangeVoucherItem> pageAdminVouchers(
        long page,
        long size,
        Long storeId,
        String status,
        String keyword
    );

    Map<String, Object> redeem(Long userId, Long storeId, String serialNo);
}
