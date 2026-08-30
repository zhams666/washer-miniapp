package com.washer.backend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.washer.backend.dto.miniadmin.MiniAdminDashboardOverview;
import com.washer.backend.dto.miniadmin.MiniAdminOrderItem;
import com.washer.backend.dto.miniadmin.MiniAdminOperationOverview;
import com.washer.backend.dto.miniadmin.MiniAdminSessionContext;
import com.washer.backend.dto.device.DeviceSimpleItem;
import java.time.LocalDate;
import java.util.List;

public interface MiniAdminPortalService {

    MiniAdminDashboardOverview getDashboard(MiniAdminSessionContext context, LocalDate bizDate, Long storeId);

    MiniAdminOperationOverview getOperationOverview(MiniAdminSessionContext context, LocalDate bizDate, Long storeId);

    List<DeviceSimpleItem> listDevices(MiniAdminSessionContext context, Long storeId, String keyword);

    DeviceSimpleItem startDevice(MiniAdminSessionContext context, Long deviceId);

    DeviceSimpleItem stopDevice(MiniAdminSessionContext context, Long deviceId);

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
