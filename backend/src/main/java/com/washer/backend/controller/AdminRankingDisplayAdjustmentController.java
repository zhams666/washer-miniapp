package com.washer.backend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.washer.backend.common.ApiResponse;
import com.washer.backend.dto.admin.AdminRankingDisplayAdjustmentCreateRequest;
import com.washer.backend.dto.admin.AdminRankingDisplayAdjustmentItem;
import com.washer.backend.dto.admin.AdminRankingDurationItem;
import com.washer.backend.service.RankingDisplayAdjustmentService;
import com.washer.backend.service.WashOrderService;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/ranking-display-adjustments")
public class AdminRankingDisplayAdjustmentController {

    private final RankingDisplayAdjustmentService rankingDisplayAdjustmentService;
    private final WashOrderService washOrderService;

    public AdminRankingDisplayAdjustmentController(
        RankingDisplayAdjustmentService rankingDisplayAdjustmentService,
        WashOrderService washOrderService
    ) {
        this.rankingDisplayAdjustmentService = rankingDisplayAdjustmentService;
        this.washOrderService = washOrderService;
    }

    @GetMapping
    public ApiResponse<Page<AdminRankingDisplayAdjustmentItem>> page(
        @RequestParam(defaultValue = "1") long page,
        @RequestParam(defaultValue = "10") long size,
        @RequestParam(required = false) String scope,
        @RequestParam(required = false) String keyword
    ) {
        return ApiResponse.success(rankingDisplayAdjustmentService.pageAdjustments(page, size, scope, keyword));
    }

    @GetMapping("/rankings")
    public ApiResponse<List<AdminRankingDurationItem>> rankings(
        @RequestParam(defaultValue = "total") String scope,
        @RequestParam(defaultValue = "100") int limit
    ) {
        return ApiResponse.success(washOrderService.listAdminDurationRanking(scope, limit));
    }

    @PostMapping
    public ApiResponse<AdminRankingDisplayAdjustmentItem> create(
        @RequestBody AdminRankingDisplayAdjustmentCreateRequest request
    ) {
        return ApiResponse.success("排行榜时长已增加", rankingDisplayAdjustmentService.createAdjustment(request));
    }

    @PostMapping("/set")
    public ApiResponse<AdminRankingDisplayAdjustmentItem> set(
        @RequestBody AdminRankingDisplayAdjustmentCreateRequest request
    ) {
        if (request == null || request.getUserId() == null) {
            throw new IllegalArgumentException("userId is required");
        }
        if (request.getDurationMinutes() == null || request.getDurationMinutes() < 0) {
            throw new IllegalArgumentException("durationMinutes must be greater than or equal to 0");
        }
        long realSeconds = washOrderService.sumUserCompletedDurationSeconds(request.getScope(), request.getUserId());
        long targetSeconds = Math.multiplyExact(request.getDurationMinutes(), 60L);
        AdminRankingDisplayAdjustmentCreateRequest adjustmentRequest = new AdminRankingDisplayAdjustmentCreateRequest();
        adjustmentRequest.setUserId(request.getUserId());
        adjustmentRequest.setScope(request.getScope());
        adjustmentRequest.setDurationSeconds(targetSeconds - realSeconds);
        adjustmentRequest.setRemark(request.getRemark());
        return ApiResponse.success("排行榜展示时长已保存", rankingDisplayAdjustmentService.setAdjustment(adjustmentRequest));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        rankingDisplayAdjustmentService.deleteAdjustment(id);
        return ApiResponse.success("排行榜时长记录已删除", null);
    }
}
