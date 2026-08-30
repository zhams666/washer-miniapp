package com.washer.backend.controller;

import com.washer.backend.common.ApiResponse;
import com.washer.backend.dto.admin.AdminDashboardActivityItem;
import com.washer.backend.dto.admin.AdminDashboardTodayOverview;
import com.washer.backend.service.AdminDashboardService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    public AdminDashboardController(AdminDashboardService adminDashboardService) {
        this.adminDashboardService = adminDashboardService;
    }

    @GetMapping("/today")
    public ApiResponse<AdminDashboardTodayOverview> getTodayOverview(
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate bizDate,
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
        @RequestParam(required = false) Long storeId
    ) {
        if (startDate != null || endDate != null) {
            return ApiResponse.success(adminDashboardService.getStatistics(startDate, endDate, storeId));
        }
        return ApiResponse.success(adminDashboardService.getTodayOverview(bizDate, storeId));
    }

    @GetMapping("/activities")
    public ApiResponse<List<AdminDashboardActivityItem>> listRecentActivities(
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
        @RequestParam(required = false) Integer limit,
        @RequestParam(required = false) Long storeId
    ) {
        return ApiResponse.success(adminDashboardService.listRecentActivities(startDate, endDate, limit, storeId));
    }
}
