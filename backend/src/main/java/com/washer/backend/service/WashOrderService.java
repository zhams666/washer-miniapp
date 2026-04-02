package com.washer.backend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.washer.backend.dto.admin.AdminOrderDetail;
import com.washer.backend.dto.admin.AdminOrderListItem;
import com.washer.backend.dto.order.SimpleOrderCreateRequest;
import com.washer.backend.dto.order.SimpleOrderItem;
import com.washer.backend.entity.WashOrder;
import com.washer.backend.entity.WashOrderPaymentDetail;
import com.washer.backend.entity.WashOrderStatusLog;
import java.time.LocalDateTime;
import java.util.List;

public interface WashOrderService extends IService<WashOrder> {

    List<SimpleOrderItem> getSimpleOrderList(Long userId, long size);

    WashOrder createSimpleOrder(SimpleOrderCreateRequest request);

    WashOrder startOrder(Long id);

    WashOrder completeOrder(Long id);

    List<WashOrderStatusLog> getStatusLogs(Long orderId);

    List<WashOrderPaymentDetail> getPaymentDetails(Long orderId);

    Page<AdminOrderListItem> pageAdminOrders(
        long page,
        long size,
        Long storeId,
        String orderStatus,
        String paymentStatus,
        String keyword,
        LocalDateTime startTime,
        LocalDateTime endTime
    );

    AdminOrderDetail getAdminOrderDetail(Long id);
}
