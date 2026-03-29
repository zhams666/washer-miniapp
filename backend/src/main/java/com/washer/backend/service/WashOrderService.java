package com.washer.backend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.washer.backend.dto.order.SimpleOrderCreateRequest;
import com.washer.backend.dto.order.SimpleOrderItem;
import com.washer.backend.entity.WashOrder;
import com.washer.backend.entity.WashOrderStatusLog;
import java.util.List;

public interface WashOrderService extends IService<WashOrder> {

    List<SimpleOrderItem> getSimpleOrderList(Long userId, long size);

    WashOrder createSimpleOrder(SimpleOrderCreateRequest request);

    WashOrder startOrder(Long id);

    WashOrder completeOrder(Long id);

    List<WashOrderStatusLog> getStatusLogs(Long orderId);
}
