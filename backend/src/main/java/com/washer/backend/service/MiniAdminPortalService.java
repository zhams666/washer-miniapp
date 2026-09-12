package com.washer.backend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.washer.backend.dto.admin.AdminRankingDisplayAdjustmentCreateRequest;
import com.washer.backend.dto.admin.AdminRankingDisplayAdjustmentItem;
import com.washer.backend.dto.admin.AdminRankingDurationItem;
import com.washer.backend.dto.miniadmin.MiniAdminDashboardOverview;
import com.washer.backend.dto.miniadmin.MiniAdminDeviceCreateRequest;
import com.washer.backend.dto.miniadmin.MiniAdminDeviceConfigRequest;
import com.washer.backend.dto.miniadmin.MiniAdminMetricDetailItem;
import com.washer.backend.dto.miniadmin.MiniAdminOrderItem;
import com.washer.backend.dto.miniadmin.MiniAdminOperationOverview;
import com.washer.backend.dto.miniadmin.MiniAdminSessionContext;
import com.washer.backend.dto.miniadmin.MiniAdminStoreSettingsItem;
import com.washer.backend.dto.miniadmin.MiniAdminStoreSettingsRequest;
import com.washer.backend.dto.device.DeviceSimpleItem;
import java.time.LocalDate;
import java.util.List;

public interface MiniAdminPortalService {

    MiniAdminDashboardOverview getDashboard(MiniAdminSessionContext context, LocalDate bizDate, Long storeId);

    MiniAdminOperationOverview getOperationOverview(MiniAdminSessionContext context, LocalDate bizDate, Long storeId);

    List<MiniAdminMetricDetailItem> listMetricDetails(
        MiniAdminSessionContext context,
        LocalDate bizDate,
        Long storeId,
        String metricKey
    );

    List<DeviceSimpleItem> listDevices(MiniAdminSessionContext context, Long storeId, String keyword);

    DeviceSimpleItem createDevice(MiniAdminSessionContext context, MiniAdminDeviceCreateRequest request);

    DeviceSimpleItem startDevice(MiniAdminSessionContext context, Long deviceId);

    DeviceSimpleItem stopDevice(MiniAdminSessionContext context, Long deviceId);

    DeviceSimpleItem operateDevice(MiniAdminSessionContext context, Long deviceId, String action);

    DeviceSimpleItem updateDeviceConfig(
        MiniAdminSessionContext context,
        Long deviceId,
        MiniAdminDeviceConfigRequest request
    );

    List<AdminRankingDurationItem> listRankingDurations(
        MiniAdminSessionContext context,
        String scope,
        int limit
    );

    Page<AdminRankingDisplayAdjustmentItem> pageRankingAdjustments(
        MiniAdminSessionContext context,
        long page,
        long size,
        String scope,
        String keyword
    );

    AdminRankingDisplayAdjustmentItem setRankingDisplayDuration(
        MiniAdminSessionContext context,
        AdminRankingDisplayAdjustmentCreateRequest request
    );

    void deleteRankingDisplayAdjustment(MiniAdminSessionContext context, Long adjustmentId);

    MiniAdminStoreSettingsItem getStoreSettings(MiniAdminSessionContext context, Long storeId);

    MiniAdminStoreSettingsItem updateStoreSettings(
        MiniAdminSessionContext context,
        Long storeId,
        MiniAdminStoreSettingsRequest request
    );

    MiniAdminStoreSettingsItem updateStoreCoverImage(
        MiniAdminSessionContext context,
        Long storeId,
        String coverImage
    );

    Page<MiniAdminOrderItem> pageOrders(
        MiniAdminSessionContext context,
        long page,
        long size,
        Long storeId,
        String orderStatus,
        String paymentStatus,
        String keyword
    );
}
