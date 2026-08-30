package com.washer.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.washer.backend.entity.PointMallProduct;
import com.washer.backend.entity.PointRedemptionOrder;
import com.washer.backend.entity.UserInfo;
import com.washer.backend.integration.points.PointFulfillmentGateway;
import com.washer.backend.integration.points.PointFulfillmentResult;
import com.washer.backend.mapper.PointMallProductMapper;
import com.washer.backend.mapper.PointRedemptionOrderMapper;
import com.washer.backend.mapper.UserInfoMapper;
import com.washer.backend.service.PointRedemptionService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Profile("!cloudbase")
public class PointRedemptionServiceImpl implements PointRedemptionService {

    private final PointMallProductMapper productMapper;
    private final PointRedemptionOrderMapper orderMapper;
    private final UserInfoMapper userInfoMapper;
    private final PointFulfillmentGateway fulfillmentGateway;
    private final JdbcTemplate jdbcTemplate;

    public PointRedemptionServiceImpl(
        PointMallProductMapper productMapper,
        PointRedemptionOrderMapper orderMapper,
        UserInfoMapper userInfoMapper,
        PointFulfillmentGateway fulfillmentGateway,
        JdbcTemplate jdbcTemplate
    ) {
        this.productMapper = productMapper;
        this.orderMapper = orderMapper;
        this.userInfoMapper = userInfoMapper;
        this.fulfillmentGateway = fulfillmentGateway;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PointRedemptionOrder redeem(Long userId, Long productId, String requestNo) {
        if (userId == null || userId <= 0 || productId == null || productId <= 0) {
            throw new IllegalArgumentException("userId and productId are required");
        }
        String normalizedRequestNo = StringUtils.hasText(requestNo) ? requestNo.trim() : null;
        if (normalizedRequestNo != null) {
            PointRedemptionOrder existing = orderMapper.selectOne(new LambdaQueryWrapper<PointRedemptionOrder>()
                .eq(PointRedemptionOrder::getUserId, userId)
                .eq(PointRedemptionOrder::getRequestNo, normalizedRequestNo)
                .last("limit 1"));
            if (existing != null) {
                return existing;
            }
        }

        Integer lockedUserId = jdbcTemplate.queryForObject(
            "SELECT id FROM user_info WHERE id = ? FOR UPDATE", Integer.class, userId
        );
        if (lockedUserId == null) {
            throw new IllegalArgumentException("用户不存在");
        }

        PointMallProduct product = productMapper.selectById(productId);
        if (product == null || !Integer.valueOf(1).equals(product.getStatus())) {
            throw new IllegalArgumentException("积分商品不可兑换");
        }
        LocalDateTime now = LocalDateTime.now();
        if ((product.getEffectiveTime() != null && product.getEffectiveTime().isAfter(now))
            || (product.getExpireTime() != null && !product.getExpireTime().isAfter(now))) {
            throw new IllegalArgumentException("积分商品不在兑换时间内");
        }
        int points = product.getPointsPrice() != null ? product.getPointsPrice() : 0;
        if (points <= 0) {
            throw new IllegalArgumentException("积分商品价格不合法");
        }
        if (product.getLimitPerUser() != null && product.getLimitPerUser() > 0) {
            Long used = orderMapper.selectCount(new LambdaQueryWrapper<PointRedemptionOrder>()
                .eq(PointRedemptionOrder::getUserId, userId)
                .eq(PointRedemptionOrder::getProductId, productId)
                .ne(PointRedemptionOrder::getFulfillmentStatus, "cancelled"));
            if (used != null && used >= product.getLimitPerUser()) {
                throw new IllegalArgumentException("已达到该商品的限兑次数");
            }
        }
        int stockChanged = jdbcTemplate.update(
            "UPDATE point_mall_product SET stock_total = stock_total - 1 WHERE id = ? AND status = 1 AND stock_total > 0 "
                + "AND (effective_time IS NULL OR effective_time <= NOW()) AND (expire_time IS NULL OR expire_time > NOW())",
            productId
        );
        if (stockChanged != 1) {
            throw new IllegalArgumentException("商品库存不足");
        }
        int pointsChanged = jdbcTemplate.update(
            "UPDATE user_info SET points = points - ? WHERE id = ? AND points >= ?",
            points, userId, points
        );
        if (pointsChanged != 1) {
            throw new IllegalArgumentException("积分不足");
        }
        PointRedemptionOrder order = new PointRedemptionOrder();
        order.setRedemptionNo("PR" + UUID.randomUUID().toString().replace("-", "").substring(0, 18));
        order.setRequestNo(normalizedRequestNo);
        order.setUserId(userId);
        order.setProductId(productId);
        order.setProductTitleSnapshot(product.getTitle());
        order.setPointsAmount(points);
        order.setFulfillmentStatus("pending");
        order.setRemark("point mall redemption");
        orderMapper.insert(order);
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
        return orderMapper.selectList(new LambdaQueryWrapper<PointRedemptionOrder>()
            .eq(PointRedemptionOrder::getUserId, userId)
            .orderByDesc(PointRedemptionOrder::getId)
            .last("limit " + Math.min(100, Math.max(1, limit))));
    }
}
