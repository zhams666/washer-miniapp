package com.washer.backend.service;

import com.washer.backend.dto.miniadmin.MiniAdminAssetOperationResult;
import com.washer.backend.dto.miniadmin.MiniAdminCardAdjustmentRequest;
import com.washer.backend.dto.miniadmin.MiniAdminSessionContext;
import com.washer.backend.dto.miniadmin.MiniAdminUserAssetSummary;
import com.washer.backend.dto.miniadmin.MiniAdminUserSearchItem;
import com.washer.backend.dto.miniadmin.MiniAdminWalletAdjustmentRequest;
import com.washer.backend.dto.miniadmin.MiniAdminWalletFineRequest;
import java.util.List;

public interface MiniAdminAssetService {

    List<MiniAdminUserSearchItem> searchUsers(MiniAdminSessionContext context, Long storeId, String keyword);

    MiniAdminUserAssetSummary getUserAssetSummary(MiniAdminSessionContext context, Long userId, Long storeId);

    MiniAdminAssetOperationResult adjustWallet(
        MiniAdminSessionContext context,
        MiniAdminWalletAdjustmentRequest request
    );

    MiniAdminAssetOperationResult createFine(
        MiniAdminSessionContext context,
        MiniAdminWalletFineRequest request
    );

    MiniAdminAssetOperationResult adjustCard(
        MiniAdminSessionContext context,
        MiniAdminCardAdjustmentRequest request
    );
}
