package com.washer.backend.cloudbase;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.washer.backend.entity.PointRedemptionOrder;
import com.washer.backend.integration.points.PointFulfillmentGateway;
import com.washer.backend.integration.points.PointFulfillmentResult;
import com.washer.backend.mapper.PointRedemptionOrderMapper;
import com.washer.backend.service.PointRedemptionService;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** CloudBase PostgreSQL implementation: stock, points, and order creation stay in one database RPC. */
@Service
@Profile("cloudbase")
public class CloudBasePointRedemptionService implements PointRedemptionService {

    private final CloudBasePgClient cloudBasePgClient;
    private final PointRedemptionOrderMapper orderMapper;
    private final PointFulfillmentGateway fulfillmentGateway;

    public CloudBasePointRedemptionService(
        CloudBasePgClient cloudBasePgClient,
        PointRedemptionOrderMapper orderMapper,
        PointFulfillmentGateway fulfillmentGateway
    ) {
        this.cloudBasePgClient = cloudBasePgClient;
        this.orderMapper = orderMapper;
        this.fulfillmentGateway = fulfillmentGateway;
    }

    @Override
    public PointRedemptionOrder redeem(Long userId, Long productId, String requestNo) {
        if (userId == null || userId <= 0 || productId == null || productId <= 0) {
            throw new IllegalArgumentException("userId and productId are required");
        }
        String normalizedRequestNo = StringUtils.hasText(requestNo) ? requestNo.trim() : null;
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("p_user_id", userId);
        parameters.put("p_product_id", productId);
        parameters.put("p_request_no", normalizedRequestNo);
        JsonNode response = cloudBasePgClient.rpc("redeem_points_product", parameters);
        JsonNode row = response.isArray() ? response.path(0) : response;
        if (row.isMissingNode() || row.isNull()) {
            throw new CloudBasePgException("积分兑换未返回订单");
        }
        long orderId = row.path("id").asLong(0);
        if (orderId <= 0) {
            throw new CloudBasePgException("积分兑换返回了无效订单");
        }
        PointRedemptionOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new CloudBasePgException("积分兑换订单读取失败");
        }
        if (!"pending".equals(order.getFulfillmentStatus())) {
            return order;
        }
        PointFulfillmentResult fulfillment = fulfillmentGateway.fulfill(order);
        order.setFulfillmentStatus(fulfillment.status());
        order.setFulfillmentReference(fulfillment.reference());
        order.setRemark(fulfillment.message());
        orderMapper.updateById(order);
        return order;
    }

    @Override
    public List<PointRedemptionOrder> listByUser(Long userId, int limit) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("userId is required");
        }
        int boundedLimit = Math.min(100, Math.max(1, limit));
        return orderMapper.selectList(new LambdaQueryWrapper<PointRedemptionOrder>()
            .eq(PointRedemptionOrder::getUserId, userId)
            .orderByDesc(PointRedemptionOrder::getId))
            .stream()
            .limit(boundedLimit)
            .toList();
    }
}
