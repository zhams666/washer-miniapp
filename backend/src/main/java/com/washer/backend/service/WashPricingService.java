package com.washer.backend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.washer.backend.dto.pricing.WashPricingSnapshot;
import com.washer.backend.entity.PricingRule;
import com.washer.backend.entity.WashOrder;
import com.washer.backend.mapper.PricingRuleMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WashPricingService {

    public static final int SNAPSHOT_VERSION = 1;

    private static final BigDecimal DEFAULT_BASE_PRICE = new BigDecimal("16.80");
    private static final int DEFAULT_BASE_MINUTES = 20;
    private static final BigDecimal DEFAULT_OVERTIME_PRICE_PER_MINUTE = new BigDecimal("0.78");
    private static final BigDecimal DEFAULT_VIP_BASE_PRICE = new BigDecimal("12.80");
    private static final BigDecimal DEFAULT_VIP_OVERTIME_PRICE_PER_MINUTE = new BigDecimal("0.58");
    private static final int DEFAULT_MEMBER_DAY_FIRST_MINUTES = 10;
    private static final BigDecimal DEFAULT_MEMBER_DAY_DISCOUNT_RATE = new BigDecimal("0.75");

    private final PricingRuleMapper pricingRuleMapper;
    private final ObjectMapper objectMapper;
    private final MembershipSettingService membershipSettingService;

    public WashPricingService(
        PricingRuleMapper pricingRuleMapper,
        ObjectMapper objectMapper,
        MembershipSettingService membershipSettingService
    ) {
        this.pricingRuleMapper = pricingRuleMapper;
        this.objectMapper = objectMapper;
        this.membershipSettingService = membershipSettingService;
    }

    public WashPricingSnapshot resolveSnapshotForStore(Long storeId) {
        return resolveSnapshotForStore(storeId, false, false);
    }

    public WashPricingSnapshot resolveSnapshotForStore(Long storeId, boolean vipMonthlyPricing) {
        return resolveSnapshotForStore(storeId, vipMonthlyPricing, false);
    }

    public WashPricingSnapshot resolveSnapshotForStore(
        Long storeId,
        boolean vipMonthlyPricing,
        boolean memberDayDiscount
    ) {
        PricingRule rule = findEnabledRule(storeId);
        WashPricingSnapshot snapshot;
        if (rule == null) {
            snapshot = vipMonthlyPricing ? defaultVipSnapshot() : defaultSnapshot();
        } else {
            BigDecimal basePrice = normalizePositiveAmount(rule.getFirstPeriodPrice(), DEFAULT_BASE_PRICE);
            Integer baseMinutes = normalizePositiveInteger(rule.getFirstPeriodMinutes(), DEFAULT_BASE_MINUTES);
            BigDecimal overtimePrice = normalizePositiveAmount(rule.getExtraPricePerMinute(), DEFAULT_OVERTIME_PRICE_PER_MINUTE);
            if (vipMonthlyPricing && Integer.valueOf(1).equals(rule.getMemberPriceEnabled())) {
                basePrice = normalizePositiveAmount(rule.getMemberFirstPeriodPrice(), defaultVipBasePrice(basePrice));
                overtimePrice = normalizePositiveAmount(rule.getMemberExtraPricePerMinute(), defaultVipOvertimePrice(overtimePrice));
            }
            snapshot = new WashPricingSnapshot(
                rule.getId(),
                basePrice,
                baseMinutes,
                overtimePrice,
                buildPricingRuleText(basePrice, baseMinutes, overtimePrice),
                SNAPSHOT_VERSION,
                vipMonthlyPricing,
                false,
                0,
                BigDecimal.ZERO
            );
        }
        return applyMemberDayDiscountIfNeeded(snapshot, !vipMonthlyPricing && memberDayDiscount);
    }

    public String resolvePricingRuleText(Long storeId) {
        return resolveSnapshotForStore(storeId).pricingRuleText();
    }

    public String resolvePricingRuleText(Long storeId, boolean vipMonthlyPricing) {
        return resolveSnapshotForStore(storeId, vipMonthlyPricing, false).pricingRuleText();
    }

    public String resolvePricingRuleText(Long storeId, boolean vipMonthlyPricing, boolean memberDayDiscount) {
        return resolveSnapshotForStore(storeId, vipMonthlyPricing, memberDayDiscount).pricingRuleText();
    }

    public boolean isMemberDay(LocalDate date) {
        return isMemberDay(date, null);
    }

    public boolean isMemberDay(LocalDate date, LocalTime time) {
        LocalDate safeDate = date != null ? date : LocalDate.now();
        return membershipSettingService.isMemberDay(safeDate, time);
    }

    public WashPricingSnapshot resolveSnapshotFromOrder(WashOrder order) {
        if (order == null || !StringUtils.hasText(order.getPricingSnapshot())) {
            return defaultSnapshot();
        }
        return parseSnapshot(order.getPricingSnapshot());
    }

    public WashPricingSnapshot parseSnapshot(String pricingSnapshot) {
        if (!StringUtils.hasText(pricingSnapshot)) {
            return defaultSnapshot();
        }
        try {
            JsonNode root = objectMapper.readTree(pricingSnapshot);
            BigDecimal basePrice = normalizePositiveAmount(
                readAmount(root, "basePrice", "baseAmount", "firstPeriodPrice"),
                DEFAULT_BASE_PRICE
            );
            Integer baseMinutes = normalizePositiveInteger(
                readInteger(root, "baseMinutes", "firstPeriodMinutes"),
                DEFAULT_BASE_MINUTES
            );
            BigDecimal overtimePrice = normalizePositiveAmount(
                readAmount(root, "overtimePricePerMinute", "extraMinuteAmount", "extraPricePerMinute"),
                DEFAULT_OVERTIME_PRICE_PER_MINUTE
            );
            String pricingRuleText = readText(root, "pricingRuleText");
            if (!StringUtils.hasText(pricingRuleText)) {
                pricingRuleText = buildPricingRuleText(basePrice, baseMinutes, overtimePrice);
            }
            Integer ruleVersion = normalizePositiveInteger(readInteger(root, "ruleVersion"), SNAPSHOT_VERSION);

            return new WashPricingSnapshot(
                readLong(root, "pricingRuleId"),
                basePrice,
                baseMinutes,
                overtimePrice,
                pricingRuleText,
                ruleVersion,
                readBoolean(root, "vipMonthlyPricing"),
                readBoolean(root, "memberDayDiscountApplied"),
                normalizeNonNegativeInteger(readInteger(root, "memberDayFirstMinutes")),
                normalizeNonNegativeAmount(readAmount(root, "memberDayFirstPrice"))
            );
        } catch (Exception ignored) {
            return defaultSnapshot();
        }
    }

    public String serializeSnapshot(WashPricingSnapshot snapshot) {
        WashPricingSnapshot safeSnapshot = snapshot != null ? snapshot : defaultSnapshot();
        try {
            return objectMapper.writeValueAsString(safeSnapshot);
        } catch (Exception ignored) {
            return "{\"pricingRuleId\":null,\"basePrice\":16.80,\"baseMinutes\":20,\"overtimePricePerMinute\":0.78,\"pricingRuleText\":\"16.8元/20分钟，超出后0.78元/分钟\",\"ruleVersion\":1}";
        }
    }

    public BigDecimal calculateAmount(WashOrder order, LocalDateTime endTime) {
        WashPricingSnapshot snapshot = resolveSnapshotFromOrder(order);
        return calculateAmount(snapshot, order != null ? order.getStartTime() : null, endTime);
    }

    public BigDecimal calculateAmount(WashPricingSnapshot snapshot, LocalDateTime startTime, LocalDateTime endTime) {
        WashPricingSnapshot safeSnapshot = snapshot != null ? snapshot : defaultSnapshot();
        long runningMinutes = calculateRunningMinutes(startTime, endTime);
        int baseMinutes = normalizePositiveInteger(safeSnapshot.baseMinutes(), DEFAULT_BASE_MINUTES);
        BigDecimal basePrice = normalizePositiveAmount(safeSnapshot.basePrice(), DEFAULT_BASE_PRICE);
        BigDecimal overtimePrice = normalizePositiveAmount(
            safeSnapshot.overtimePricePerMinute(),
            DEFAULT_OVERTIME_PRICE_PER_MINUTE
        );

        if (Boolean.TRUE.equals(safeSnapshot.memberDayDiscountApplied())) {
            return calculateMemberDayAmount(
                runningMinutes,
                basePrice,
                baseMinutes,
                overtimePrice,
                safeSnapshot.memberDayFirstMinutes(),
                safeSnapshot.memberDayFirstPrice()
            );
        }

        if (runningMinutes <= baseMinutes) {
            return basePrice.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal extraAmount = overtimePrice.multiply(BigDecimal.valueOf(runningMinutes - baseMinutes));
        return basePrice.add(extraAmount).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getBasePrice(WashPricingSnapshot snapshot) {
        WashPricingSnapshot safeSnapshot = snapshot != null ? snapshot : defaultSnapshot();
        if (Boolean.TRUE.equals(safeSnapshot.memberDayDiscountApplied())) {
            return normalizePositiveAmount(safeSnapshot.memberDayFirstPrice(), DEFAULT_BASE_PRICE)
                .setScale(2, RoundingMode.HALF_UP);
        }
        return normalizePositiveAmount(safeSnapshot.basePrice(), DEFAULT_BASE_PRICE).setScale(2, RoundingMode.HALF_UP);
    }

    private PricingRule findEnabledRule(Long storeId) {
        if (storeId == null) {
            return null;
        }
        return pricingRuleMapper.selectOne(
            new LambdaQueryWrapper<PricingRule>()
                .eq(PricingRule::getStoreId, storeId)
                .eq(PricingRule::getStatus, 1)
                .orderByDesc(PricingRule::getIsDefault)
                .orderByDesc(PricingRule::getId)
                .last("limit 1")
        );
    }

    private WashPricingSnapshot defaultSnapshot() {
        return new WashPricingSnapshot(
            null,
            DEFAULT_BASE_PRICE,
            DEFAULT_BASE_MINUTES,
            DEFAULT_OVERTIME_PRICE_PER_MINUTE,
            buildPricingRuleText(DEFAULT_BASE_PRICE, DEFAULT_BASE_MINUTES, DEFAULT_OVERTIME_PRICE_PER_MINUTE),
            SNAPSHOT_VERSION,
            false,
            false,
            0,
            BigDecimal.ZERO
        );
    }

    private WashPricingSnapshot defaultVipSnapshot() {
        return new WashPricingSnapshot(
            null,
            DEFAULT_VIP_BASE_PRICE,
            DEFAULT_BASE_MINUTES,
            DEFAULT_VIP_OVERTIME_PRICE_PER_MINUTE,
            buildPricingRuleText(DEFAULT_VIP_BASE_PRICE, DEFAULT_BASE_MINUTES, DEFAULT_VIP_OVERTIME_PRICE_PER_MINUTE),
            SNAPSHOT_VERSION,
            true,
            false,
            0,
            BigDecimal.ZERO
        );
    }

    private WashPricingSnapshot applyMemberDayDiscountIfNeeded(WashPricingSnapshot snapshot, boolean memberDayDiscount) {
        if (!memberDayDiscount || snapshot == null) {
            return snapshot;
        }
        int firstMinutes = Math.min(
            membershipSettingService.firstMinutes(),
            normalizePositiveInteger(snapshot.baseMinutes(), DEFAULT_BASE_MINUTES)
        );
        BigDecimal firstPrice = resolveMemberDayFirstPrice(snapshot.basePrice(), snapshot.baseMinutes(), firstMinutes);
        return new WashPricingSnapshot(
            snapshot.pricingRuleId(),
            snapshot.basePrice(),
            snapshot.baseMinutes(),
            snapshot.overtimePricePerMinute(),
            snapshot.pricingRuleText() + "；会员日前" + firstMinutes + "分钟" + formatAmount(firstPrice) + "元",
            snapshot.ruleVersion(),
            Boolean.TRUE.equals(snapshot.vipMonthlyPricing()),
            true,
            firstMinutes,
            firstPrice
        );
    }

    private BigDecimal calculateMemberDayAmount(
        long runningMinutes,
        BigDecimal basePrice,
        int baseMinutes,
        BigDecimal overtimePrice,
        Integer memberDayFirstMinutes,
        BigDecimal memberDayFirstPrice
    ) {
        int firstMinutes = normalizePositiveInteger(memberDayFirstMinutes, DEFAULT_MEMBER_DAY_FIRST_MINUTES);
        firstMinutes = Math.min(firstMinutes, Math.max(1, baseMinutes));
        BigDecimal firstPrice = normalizePositiveAmount(
            memberDayFirstPrice,
            resolveMemberDayFirstPrice(basePrice, baseMinutes, firstMinutes)
        );
        BigDecimal normalFirstSegmentPrice = basePrice
            .multiply(BigDecimal.valueOf(firstMinutes))
            .divide(BigDecimal.valueOf(Math.max(1, baseMinutes)), 2, RoundingMode.HALF_UP);
        BigDecimal discountedBasePrice = basePrice.subtract(normalFirstSegmentPrice.subtract(firstPrice));
        if (discountedBasePrice.compareTo(firstPrice) < 0) {
            discountedBasePrice = firstPrice;
        }

        if (runningMinutes <= firstMinutes) {
            return firstPrice.setScale(2, RoundingMode.HALF_UP);
        }
        if (runningMinutes <= baseMinutes) {
            return discountedBasePrice.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal extraAmount = overtimePrice.multiply(BigDecimal.valueOf(runningMinutes - baseMinutes));
        return discountedBasePrice.add(extraAmount).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal resolveMemberDayFirstPrice(BigDecimal basePrice, Integer baseMinutes, int firstMinutes) {
        BigDecimal normalBase = normalizePositiveAmount(basePrice, DEFAULT_BASE_PRICE);
        int normalMinutes = normalizePositiveInteger(baseMinutes, DEFAULT_BASE_MINUTES);
        BigDecimal normalFirstSegmentPrice = normalBase
            .multiply(BigDecimal.valueOf(firstMinutes))
            .divide(BigDecimal.valueOf(Math.max(1, normalMinutes)), 2, RoundingMode.HALF_UP);
        BigDecimal discountRate = membershipSettingService.discountRate();
        if (discountRate == null || discountRate.compareTo(BigDecimal.ZERO) <= 0 || discountRate.compareTo(BigDecimal.ONE) > 0) {
            discountRate = DEFAULT_MEMBER_DAY_DISCOUNT_RATE;
        }
        BigDecimal discounted = normalFirstSegmentPrice.multiply(discountRate);
        return discounted.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal defaultVipBasePrice(BigDecimal normalBasePrice) {
        BigDecimal normal = normalizePositiveAmount(normalBasePrice, DEFAULT_BASE_PRICE);
        BigDecimal discounted = normal.multiply(new BigDecimal("0.80")).setScale(2, RoundingMode.HALF_UP);
        return discounted.compareTo(BigDecimal.ZERO) > 0 ? discounted : DEFAULT_VIP_BASE_PRICE;
    }

    private BigDecimal defaultVipOvertimePrice(BigDecimal normalOvertimePrice) {
        BigDecimal normal = normalizePositiveAmount(normalOvertimePrice, DEFAULT_OVERTIME_PRICE_PER_MINUTE);
        BigDecimal discounted = normal.multiply(new BigDecimal("0.75")).setScale(2, RoundingMode.HALF_UP);
        return discounted.compareTo(BigDecimal.ZERO) > 0 ? discounted : DEFAULT_VIP_OVERTIME_PRICE_PER_MINUTE;
    }

    private String buildPricingRuleText(BigDecimal basePrice, Integer baseMinutes, BigDecimal overtimePrice) {
        return formatAmount(basePrice) + "元/" + baseMinutes + "分钟，超出后" + formatAmount(overtimePrice) + "元/分钟";
    }

    private String formatAmount(BigDecimal amount) {
        BigDecimal safeAmount = amount != null ? amount : BigDecimal.ZERO;
        return safeAmount.stripTrailingZeros().toPlainString();
    }

    private long calculateRunningMinutes(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null || endTime == null || endTime.isBefore(startTime)) {
            return 0L;
        }
        return Math.max(0L, Duration.between(startTime, endTime).toMinutes());
    }

    private BigDecimal normalizePositiveAmount(BigDecimal value, BigDecimal fallback) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            return fallback;
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal normalizeNonNegativeAmount(BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private Integer normalizePositiveInteger(Integer value, Integer fallback) {
        if (value == null || value <= 0) {
            return fallback;
        }
        return value;
    }

    private Integer normalizeNonNegativeInteger(Integer value) {
        if (value == null || value < 0) {
            return 0;
        }
        return value;
    }

    private BigDecimal readAmount(JsonNode root, String... fields) {
        JsonNode value = readNode(root, fields);
        if (value == null || value.isNull()) {
            return null;
        }
        if (value.isNumber()) {
            return value.decimalValue();
        }
        if (value.isTextual() && StringUtils.hasText(value.asText())) {
            try {
                return new BigDecimal(value.asText().trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private Integer readInteger(JsonNode root, String... fields) {
        JsonNode value = readNode(root, fields);
        if (value == null || value.isNull()) {
            return null;
        }
        if (value.isInt() || value.isLong()) {
            return value.asInt();
        }
        if (value.isTextual() && StringUtils.hasText(value.asText())) {
            try {
                return Integer.parseInt(value.asText().trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private Long readLong(JsonNode root, String... fields) {
        JsonNode value = readNode(root, fields);
        if (value == null || value.isNull()) {
            return null;
        }
        if (value.isNumber()) {
            return value.asLong();
        }
        if (value.isTextual() && StringUtils.hasText(value.asText())) {
            try {
                return Long.parseLong(value.asText().trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private Boolean readBoolean(JsonNode root, String... fields) {
        JsonNode value = readNode(root, fields);
        if (value == null || value.isNull()) {
            return false;
        }
        if (value.isBoolean()) {
            return value.asBoolean();
        }
        if (value.isNumber()) {
            return value.asInt() != 0;
        }
        if (value.isTextual()) {
            String text = value.asText();
            return "true".equalsIgnoreCase(text) || "1".equals(text);
        }
        return false;
    }

    private String readText(JsonNode root, String... fields) {
        JsonNode value = readNode(root, fields);
        if (value == null || value.isNull()) {
            return null;
        }
        return value.asText();
    }

    private JsonNode readNode(JsonNode root, String... fields) {
        if (root == null || fields == null) {
            return null;
        }
        for (String field : fields) {
            if (root.hasNonNull(field)) {
                return root.get(field);
            }
        }
        return null;
    }
}
