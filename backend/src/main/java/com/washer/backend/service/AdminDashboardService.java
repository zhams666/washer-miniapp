package com.washer.backend.service;

import com.washer.backend.dto.admin.AdminDashboardActivityItem;
import com.washer.backend.dto.admin.AdminDashboardTodayOverview;
import java.time.LocalDate;
import java.util.List;

public interface AdminDashboardService {

    AdminDashboardTodayOverview getTodayOverview(LocalDate bizDate);

    AdminDashboardTodayOverview getTodayOverview(LocalDate bizDate, Long storeId);

    AdminDashboardTodayOverview getStatistics(LocalDate startDate, LocalDate endDate);

    AdminDashboardTodayOverview getStatistics(LocalDate startDate, LocalDate endDate, Long storeId);

    List<AdminDashboardActivityItem> listRecentActivities(LocalDate startDate, LocalDate endDate, Integer limit);

    List<AdminDashboardActivityItem> listRecentActivities(LocalDate startDate, LocalDate endDate, Integer limit, Long storeId);
}
