package com.washer.backend.dto.admin;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class AdminDashboardTodayOverview {

    private LocalDate bizDate;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDateTime generatedAt;
    private List<AdminDashboardKpiItem> topMetrics = new ArrayList<>();
    private List<AdminDashboardKpiItem> rangeMetrics = new ArrayList<>();
    private List<AdminDashboardDailyTrendPoint> dailyTrend = new ArrayList<>();
    private List<AdminDashboardMetric> summaryCards = new ArrayList<>();
    private List<AdminDashboardHourlyPoint> hourlyTrend = new ArrayList<>();
    private List<AdminDashboardStoreMetric> storeMetrics = new ArrayList<>();
    private List<AdminDashboardChannelMetric> cardPurchaseChannels = new ArrayList<>();
    private AdminDashboardDeviceStatus deviceStatus = new AdminDashboardDeviceStatus();
    private List<AdminDashboardActivityItem> recentActivities = new ArrayList<>();
}
