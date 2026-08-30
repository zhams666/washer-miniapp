package com.washer.backend.controller;

import com.washer.backend.common.ApiResponse;
import com.washer.backend.dto.miniadmin.MiniAdminAssetOperationResult;
import com.washer.backend.dto.miniadmin.MiniAdminCardAdjustmentRequest;
import com.washer.backend.dto.miniadmin.MiniAdminSessionContext;
import com.washer.backend.dto.miniadmin.MiniAdminUserAssetSummary;
import com.washer.backend.dto.miniadmin.MiniAdminUserSearchItem;
import com.washer.backend.dto.miniadmin.MiniAdminWalletAdjustmentRequest;
import com.washer.backend.dto.miniadmin.MiniAdminWalletFineRequest;
import com.washer.backend.service.MiniAdminAssetService;
import com.washer.backend.service.MiniAdminAuthService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mini-admin")
public class MiniAdminAssetController {

    private final MiniAdminAuthService miniAdminAuthService;
    private final MiniAdminAssetService miniAdminAssetService;

    public MiniAdminAssetController(
        MiniAdminAuthService miniAdminAuthService,
        MiniAdminAssetService miniAdminAssetService
    ) {
        this.miniAdminAuthService = miniAdminAuthService;
        this.miniAdminAssetService = miniAdminAssetService;
    }

    @GetMapping("/users/search")
    public ApiResponse<List<MiniAdminUserSearchItem>> searchUsers(
        @RequestHeader(value = "X-Washer-Admin-Token", required = false) String token,
        @RequestParam(required = false) Long storeId,
        @RequestParam(required = false) String keyword
    ) {
        MiniAdminSessionContext context = miniAdminAuthService.requireContext(token);
        return ApiResponse.success(miniAdminAssetService.searchUsers(context, storeId, keyword));
    }

    @GetMapping("/users/{userId}/assets")
    public ApiResponse<MiniAdminUserAssetSummary> userAssets(
        @RequestHeader(value = "X-Washer-Admin-Token", required = false) String token,
        @PathVariable Long userId,
        @RequestParam Long storeId
    ) {
        MiniAdminSessionContext context = miniAdminAuthService.requireContext(token);
        return ApiResponse.success(miniAdminAssetService.getUserAssetSummary(context, userId, storeId));
    }

    @PostMapping("/asset/wallet-adjustments")
    public ApiResponse<MiniAdminAssetOperationResult> adjustWallet(
        @RequestHeader(value = "X-Washer-Admin-Token", required = false) String token,
        @RequestBody MiniAdminWalletAdjustmentRequest request
    ) {
        MiniAdminSessionContext context = miniAdminAuthService.requireContext(token);
        return ApiResponse.success(miniAdminAssetService.adjustWallet(context, request));
    }

    @PostMapping("/asset/wallet-fines")
    public ApiResponse<MiniAdminAssetOperationResult> createFine(
        @RequestHeader(value = "X-Washer-Admin-Token", required = false) String token,
        @RequestBody MiniAdminWalletFineRequest request
    ) {
        MiniAdminSessionContext context = miniAdminAuthService.requireContext(token);
        return ApiResponse.success(miniAdminAssetService.createFine(context, request));
    }

    @PostMapping("/asset/card-adjustments")
    public ApiResponse<MiniAdminAssetOperationResult> adjustCard(
        @RequestHeader(value = "X-Washer-Admin-Token", required = false) String token,
        @RequestBody MiniAdminCardAdjustmentRequest request
    ) {
        MiniAdminSessionContext context = miniAdminAuthService.requireContext(token);
        return ApiResponse.success(miniAdminAssetService.adjustCard(context, request));
    }
}
