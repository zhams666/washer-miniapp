package com.washer.backend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.washer.backend.dto.admin.AdminRankingDisplayAdjustmentCreateRequest;
import com.washer.backend.dto.admin.AdminRankingDisplayAdjustmentItem;
import com.washer.backend.entity.RankingDisplayAdjustment;
import java.time.LocalDateTime;
import java.util.List;

public interface RankingDisplayAdjustmentService {

    Page<AdminRankingDisplayAdjustmentItem> pageAdjustments(long page, long size, String scope, String keyword);

    AdminRankingDisplayAdjustmentItem createAdjustment(AdminRankingDisplayAdjustmentCreateRequest request);

    AdminRankingDisplayAdjustmentItem setAdjustment(AdminRankingDisplayAdjustmentCreateRequest request);

    void deleteAdjustment(Long id);

    List<RankingDisplayAdjustment> listForRanking(String scope, LocalDateTime fromTime, LocalDateTime toTime);
}
