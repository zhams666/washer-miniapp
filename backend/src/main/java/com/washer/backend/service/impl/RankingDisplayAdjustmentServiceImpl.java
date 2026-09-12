package com.washer.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.washer.backend.dto.admin.AdminRankingDisplayAdjustmentCreateRequest;
import com.washer.backend.dto.admin.AdminRankingDisplayAdjustmentItem;
import com.washer.backend.entity.RankingDisplayAdjustment;
import com.washer.backend.entity.UserInfo;
import com.washer.backend.mapper.RankingDisplayAdjustmentMapper;
import com.washer.backend.mapper.UserInfoMapper;
import com.washer.backend.service.RankingDisplayAdjustmentService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class RankingDisplayAdjustmentServiceImpl implements RankingDisplayAdjustmentService {

    private static final long MAX_DURATION_MINUTES = 100_000L;
    private static final int MAX_PAGE_SIZE = 100;
    private static final int USER_KEYWORD_RESULT_LIMIT = 500;

    private final RankingDisplayAdjustmentMapper rankingDisplayAdjustmentMapper;
    private final UserInfoMapper userInfoMapper;

    public RankingDisplayAdjustmentServiceImpl(
        RankingDisplayAdjustmentMapper rankingDisplayAdjustmentMapper,
        UserInfoMapper userInfoMapper
    ) {
        this.rankingDisplayAdjustmentMapper = rankingDisplayAdjustmentMapper;
        this.userInfoMapper = userInfoMapper;
    }

    @Override
    public Page<AdminRankingDisplayAdjustmentItem> pageAdjustments(long page, long size, String scope, String keyword) {
        long currentPage = Math.max(1L, page);
        long pageSize = Math.max(1L, Math.min(size, MAX_PAGE_SIZE));
        String resolvedScope = normalizeScope(scope);
        LambdaQueryWrapper<RankingDisplayAdjustment> wrapper = new LambdaQueryWrapper<RankingDisplayAdjustment>()
            .eq(StringUtils.hasText(resolvedScope), RankingDisplayAdjustment::getScope, resolvedScope)
            .orderByDesc(RankingDisplayAdjustment::getOccurredAt)
            .orderByDesc(RankingDisplayAdjustment::getId);

        if (StringUtils.hasText(keyword)) {
            List<Long> userIds = findUserIdsByKeyword(keyword.trim());
            if (userIds.isEmpty()) {
                return new Page<>(currentPage, pageSize, 0L);
            }
            wrapper.in(RankingDisplayAdjustment::getUserId, userIds);
        }

        Page<RankingDisplayAdjustment> adjustmentPage = new Page<>(currentPage, pageSize);
        rankingDisplayAdjustmentMapper.selectPage(adjustmentPage, wrapper);
        Map<Long, UserInfo> userMap = buildUserMap(adjustmentPage.getRecords());
        Page<AdminRankingDisplayAdjustmentItem> result = new Page<>(
            adjustmentPage.getCurrent(),
            adjustmentPage.getSize(),
            adjustmentPage.getTotal()
        );
        result.setRecords(adjustmentPage.getRecords().stream()
            .map(item -> toItem(item, userMap.get(item.getUserId())))
            .toList());
        return result;
    }

    @Override
    public AdminRankingDisplayAdjustmentItem createAdjustment(AdminRankingDisplayAdjustmentCreateRequest request) {
        if (request == null || request.getUserId() == null) {
            throw new IllegalArgumentException("userId is required");
        }
        long durationSeconds = normalizeDurationSeconds(request, false, false);
        String scope = normalizeScope(request.getScope());
        UserInfo user = userInfoMapper.selectById(request.getUserId());
        if (user == null) {
            throw new IllegalArgumentException("user does not exist");
        }

        RankingDisplayAdjustment adjustment = buildAdjustment(user.getId(), scope, durationSeconds, request.getRemark());
        rankingDisplayAdjustmentMapper.insert(adjustment);
        return toItem(adjustment, user);
    }

    @Override
    public AdminRankingDisplayAdjustmentItem setAdjustment(AdminRankingDisplayAdjustmentCreateRequest request) {
        if (request == null || request.getUserId() == null) {
            throw new IllegalArgumentException("userId is required");
        }
        long durationSeconds = normalizeDurationSeconds(request, true, true);
        String scope = normalizeScope(request.getScope());
        UserInfo user = userInfoMapper.selectById(request.getUserId());
        if (user == null) {
            throw new IllegalArgumentException("user does not exist");
        }
        rankingDisplayAdjustmentMapper.delete(
            new LambdaQueryWrapper<RankingDisplayAdjustment>()
                .eq(RankingDisplayAdjustment::getUserId, user.getId())
                .eq(RankingDisplayAdjustment::getScope, scope)
        );

        RankingDisplayAdjustment adjustment = buildAdjustment(user.getId(), scope, durationSeconds, request.getRemark());
        rankingDisplayAdjustmentMapper.insert(adjustment);
        return toItem(adjustment, user);
    }

    @Override
    public void deleteAdjustment(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("adjustment id is required");
        }
        if (rankingDisplayAdjustmentMapper.deleteById(id) == 0) {
            throw new IllegalArgumentException("ranking adjustment does not exist");
        }
    }

    @Override
    public List<RankingDisplayAdjustment> listForRanking(String scope, LocalDateTime fromTime, LocalDateTime toTime) {
        String resolvedScope = normalizeScope(scope);
        return rankingDisplayAdjustmentMapper.selectList(
            new LambdaQueryWrapper<RankingDisplayAdjustment>()
                .in(RankingDisplayAdjustment::getScope, scopesIncludedBy(resolvedScope))
                .ge(fromTime != null, RankingDisplayAdjustment::getOccurredAt, fromTime)
                .le(toTime != null, RankingDisplayAdjustment::getOccurredAt, toTime)
                .orderByDesc(RankingDisplayAdjustment::getOccurredAt)
        );
    }

    private List<String> scopesIncludedBy(String scope) {
        return switch (scope) {
            case "day" -> List.of("day");
            case "month" -> List.of("day", "month");
            default -> List.of("day", "month", "total");
        };
    }

    private List<Long> findUserIdsByKeyword(String keyword) {
        return userInfoMapper.selectList(
            new LambdaQueryWrapper<UserInfo>()
                .and(wrapper -> wrapper
                    .like(UserInfo::getNickname, keyword)
                    .or()
                    .like(UserInfo::getMobile, keyword)
                    .or()
                    .like(UserInfo::getUserNo, keyword)
                    .or()
                    .like(UserInfo::getRealName, keyword))
                .orderByDesc(UserInfo::getId)
                .last("limit " + USER_KEYWORD_RESULT_LIMIT)
        ).stream().map(UserInfo::getId).toList();
    }

    private Map<Long, UserInfo> buildUserMap(List<RankingDisplayAdjustment> adjustments) {
        List<Long> userIds = adjustments.stream()
            .map(RankingDisplayAdjustment::getUserId)
            .filter(id -> id != null)
            .distinct()
            .toList();
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return userInfoMapper.selectBatchIds(userIds).stream()
            .collect(Collectors.toMap(UserInfo::getId, Function.identity(), (a, b) -> a));
    }

    private RankingDisplayAdjustment buildAdjustment(Long userId, String scope, long durationSeconds, String remark) {
        LocalDateTime now = LocalDateTime.now();
        RankingDisplayAdjustment adjustment = new RankingDisplayAdjustment();
        adjustment.setUserId(userId);
        adjustment.setScope(scope);
        adjustment.setDisplayDurationSeconds(durationSeconds);
        adjustment.setRemark(normalizeRemark(remark));
        adjustment.setOccurredAt(now);
        adjustment.setCreatedAt(now);
        adjustment.setUpdatedAt(now);
        return adjustment;
    }

    private long normalizeDurationSeconds(
        AdminRankingDisplayAdjustmentCreateRequest request,
        boolean allowZero,
        boolean allowNegative
    ) {
        Long secondsValue = request.getDurationSeconds();
        if (secondsValue != null) {
            long maxSeconds = Math.multiplyExact(MAX_DURATION_MINUTES, 60L);
            if ((!allowNegative && secondsValue < 0) || Math.abs(secondsValue) > maxSeconds) {
                throw new IllegalArgumentException("durationSeconds is invalid");
            }
            if (!allowZero && secondsValue == 0) {
                throw new IllegalArgumentException("durationSeconds must not be 0");
            }
            return secondsValue;
        }

        Long value = request.getDurationMinutes();
        if (value == null || (!allowNegative && value < 0) || (!allowZero && value == 0)) {
            throw new IllegalArgumentException(
                allowZero ? "durationMinutes must be greater than or equal to 0" : "durationMinutes must be greater than 0"
            );
        }
        if (Math.abs(value) > MAX_DURATION_MINUTES) {
            throw new IllegalArgumentException("durationMinutes cannot exceed " + MAX_DURATION_MINUTES);
        }
        return Math.multiplyExact(value, 60L);
    }

    private String normalizeRemark(String remark) {
        if (!StringUtils.hasText(remark)) {
            return "";
        }
        String value = remark.trim();
        if (value.length() > 255) {
            throw new IllegalArgumentException("remark cannot exceed 255 characters");
        }
        return value;
    }

    private AdminRankingDisplayAdjustmentItem toItem(RankingDisplayAdjustment adjustment, UserInfo user) {
        long seconds = adjustment.getDisplayDurationSeconds() != null ? adjustment.getDisplayDurationSeconds() : 0L;
        long minutes = toSignedDisplayMinutes(seconds);
        String scope = normalizeScope(adjustment.getScope());
        return new AdminRankingDisplayAdjustmentItem(
            adjustment.getId(),
            adjustment.getUserId(),
            scope,
            resolveScopeName(scope),
            user != null ? user.getUserNo() : "",
            user != null ? user.getNickname() : "",
            user != null ? user.getMobile() : "",
            seconds,
            minutes,
            formatSignedDurationText(seconds),
            adjustment.getRemark(),
            adjustment.getOccurredAt(),
            adjustment.getCreatedAt()
        );
    }

    private String formatDurationText(long seconds) {
        long minutes = seconds <= 0 ? 0L : (seconds + 59L) / 60L;
        long hours = minutes / 60L;
        long remainMinutes = minutes % 60L;
        return hours > 0 ? String.format("%02d时%02d分", hours, remainMinutes) : String.format("%02d分", remainMinutes);
    }

    private long toSignedDisplayMinutes(long seconds) {
        if (seconds == 0) {
            return 0L;
        }
        long minutes = (Math.abs(seconds) + 59L) / 60L;
        return seconds < 0 ? -minutes : minutes;
    }

    private String formatSignedDurationText(long seconds) {
        if (seconds < 0) {
            return "-" + formatDurationText(Math.abs(seconds));
        }
        return formatDurationText(seconds);
    }

    private String normalizeScope(String scope) {
        if ("day".equalsIgnoreCase(scope)) {
            return "day";
        }
        if ("month".equalsIgnoreCase(scope)) {
            return "month";
        }
        return "total";
    }

    private String resolveScopeName(String scope) {
        return switch (normalizeScope(scope)) {
            case "day" -> "24小时榜";
            case "month" -> "30日榜";
            default -> "总榜";
        };
    }
}
