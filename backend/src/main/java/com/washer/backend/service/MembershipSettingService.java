package com.washer.backend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.washer.backend.entity.MembershipSetting;
import com.washer.backend.mapper.MembershipSettingMapper;
import java.math.BigDecimal;
import java.time.LocalTime;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class MembershipSettingService {

    private static final String DEFAULT_KEY = "default";
    private static final int DEFAULT_WEEKDAY = 3;
    private static final int DEFAULT_FIRST_MINUTES = 10;
    private static final BigDecimal DEFAULT_DISCOUNT_RATE = new BigDecimal("0.7500");

    private final MembershipSettingMapper settingMapper;

    public MembershipSettingService(MembershipSettingMapper settingMapper) {
        this.settingMapper = settingMapper;
    }

    public MembershipSetting getSettings() {
        MembershipSetting settings = settingMapper.selectOne(
            new LambdaQueryWrapper<MembershipSetting>()
                .eq(MembershipSetting::getSettingKey, DEFAULT_KEY)
                .last("limit 1")
        );
        if (settings != null) {
            return normalize(settings);
        }

        MembershipSetting defaults = new MembershipSetting();
        defaults.setSettingKey(DEFAULT_KEY);
        defaults.setMemberDayEnabled(1);
        defaults.setMemberDayWeekday(DEFAULT_WEEKDAY);
        defaults.setMemberDayStartTime(LocalTime.MIDNIGHT);
        defaults.setMemberDayEndTime(LocalTime.of(23, 59, 59));
        defaults.setMemberDayFirstMinutes(DEFAULT_FIRST_MINUTES);
        defaults.setMemberDayDiscountRate(DEFAULT_DISCOUNT_RATE);
        defaults.setBenefitText("会员日享首段洗车优惠，月会员和年会员按有效期享受会员权益");
        settingMapper.insert(defaults);
        return defaults;
    }

    public MembershipSetting saveSettings(MembershipSetting input) {
        MembershipSetting current = getSettings();
        if (input != null) {
            current.setMemberDayEnabled(input.getMemberDayEnabled());
            current.setMemberDayWeekday(input.getMemberDayWeekday());
            current.setMemberDayStartTime(input.getMemberDayStartTime());
            current.setMemberDayEndTime(input.getMemberDayEndTime());
            current.setMemberDayFirstMinutes(input.getMemberDayFirstMinutes());
            current.setMemberDayDiscountRate(input.getMemberDayDiscountRate());
            current.setBenefitText(input.getBenefitText());
        }
        normalize(current);
        settingMapper.updateById(current);
        return current;
    }

    public boolean isMemberDay(java.time.LocalDate date, LocalTime time) {
        MembershipSetting settings = getSettings();
        if (!Integer.valueOf(1).equals(settings.getMemberDayEnabled()) || date == null) {
            return false;
        }
        if (!Integer.valueOf(date.getDayOfWeek().getValue()).equals(settings.getMemberDayWeekday())) {
            return false;
        }
        if (time == null) {
            return true;
        }
        LocalTime start = settings.getMemberDayStartTime();
        LocalTime end = settings.getMemberDayEndTime();
        if (end.isBefore(start)) {
            return !time.isBefore(start) || !time.isAfter(end);
        }
        return !time.isBefore(start) && !time.isAfter(end);
    }

    public int firstMinutes() {
        return getSettings().getMemberDayFirstMinutes();
    }

    public BigDecimal discountRate() {
        return getSettings().getMemberDayDiscountRate();
    }

    private MembershipSetting normalize(MembershipSetting settings) {
        settings.setSettingKey(StringUtils.hasText(settings.getSettingKey()) ? settings.getSettingKey() : DEFAULT_KEY);
        settings.setMemberDayEnabled(Integer.valueOf(1).equals(settings.getMemberDayEnabled()) ? 1 : 0);
        int weekday = settings.getMemberDayWeekday() == null ? DEFAULT_WEEKDAY : settings.getMemberDayWeekday();
        settings.setMemberDayWeekday(Math.max(1, Math.min(7, weekday)));
        settings.setMemberDayStartTime(settings.getMemberDayStartTime() != null ? settings.getMemberDayStartTime() : LocalTime.MIDNIGHT);
        settings.setMemberDayEndTime(settings.getMemberDayEndTime() != null ? settings.getMemberDayEndTime() : LocalTime.of(23, 59, 59));
        int firstMinutes = settings.getMemberDayFirstMinutes() == null ? DEFAULT_FIRST_MINUTES : settings.getMemberDayFirstMinutes();
        settings.setMemberDayFirstMinutes(Math.max(1, Math.min(120, firstMinutes)));
        BigDecimal rate = settings.getMemberDayDiscountRate();
        if (rate == null || rate.compareTo(BigDecimal.ZERO) <= 0 || rate.compareTo(BigDecimal.ONE) > 0) {
            rate = DEFAULT_DISCOUNT_RATE;
        }
        settings.setMemberDayDiscountRate(rate.setScale(4, java.math.RoundingMode.HALF_UP));
        if (!StringUtils.hasText(settings.getBenefitText())) {
            settings.setBenefitText("会员日享首段洗车优惠，月会员和年会员按有效期享受会员权益");
        }
        return settings;
    }
}
