package com.washer.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.washer.backend.dto.queue.WashQueueRequest;
import com.washer.backend.entity.Store;
import com.washer.backend.entity.WashQueue;
import com.washer.backend.mapper.WashQueueMapper;
import com.washer.backend.service.StoreService;
import com.washer.backend.service.WashQueueService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WashQueueServiceImpl extends ServiceImpl<WashQueueMapper, WashQueue> implements WashQueueService {

    private static final String STATUS_WAITING = "waiting";
    private static final String STATUS_CANCELLED = "cancelled";
    private static final double MAX_QUEUE_DISTANCE_KM = 0.1D;

    private final StoreService storeService;

    public WashQueueServiceImpl(StoreService storeService) {
        this.storeService = storeService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> joinQueue(WashQueueRequest request) {
        validateRequest(request);
        Store store = getRequiredStore(request.getStoreId());
        double distanceKm = resolveDistanceKm(store, request.getUserLat(), request.getUserLng());
        WashQueue existing = findWaitingQueue(request.getUserId(), request.getStoreId());
        if (distanceKm > MAX_QUEUE_DISTANCE_KM) {
            if (existing != null) {
                cancelQueue(existing, distanceKm, "out_of_range");
            }
            throw new IllegalArgumentException("queue distance is over 100m");
        }

        if (existing == null) {
            existing = new WashQueue();
            existing.setQueueNo("Q" + UUID.randomUUID().toString().replace("-", "").substring(0, 18));
            existing.setUserId(request.getUserId());
            existing.setStoreId(request.getStoreId());
            existing.setQueueStatus(STATUS_WAITING);
            existing.setQueuedAt(LocalDateTime.now());
            this.save(existing);
        }

        updateQueueLocation(existing, request.getUserLat(), request.getUserLng(), distanceKm);
        WashQueue latest = findWaitingQueue(request.getUserId(), request.getStoreId());
        return buildQueueStatus(latest, distanceKm);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> checkQueueLocation(WashQueueRequest request) {
        validateRequest(request);
        Store store = getRequiredStore(request.getStoreId());
        double distanceKm = resolveDistanceKm(store, request.getUserLat(), request.getUserLng());
        WashQueue queue = findWaitingQueue(request.getUserId(), request.getStoreId());
        if (queue == null) {
            return buildEmptyStatus(request.getStoreId(), distanceKm);
        }

        if (distanceKm > MAX_QUEUE_DISTANCE_KM) {
            cancelQueue(queue, distanceKm, "out_of_range");
            Map<String, Object> result = buildEmptyStatus(request.getStoreId(), distanceKm);
            result.put("cancelled", true);
            result.put("cancelReason", "out_of_range");
            return result;
        }

        updateQueueLocation(queue, request.getUserLat(), request.getUserLng(), distanceKm);
        return buildQueueStatus(findWaitingQueue(request.getUserId(), request.getStoreId()), distanceKm);
    }

    @Override
    public Map<String, Object> getQueueStatus(Long userId, Long storeId) {
        if (userId == null || storeId == null) {
            return buildEmptyStatus(storeId, null);
        }
        return buildQueueStatus(findWaitingQueue(userId, storeId), null);
    }

    private void validateRequest(WashQueueRequest request) {
        if (request == null || request.getUserId() == null) {
            throw new IllegalArgumentException("userId is required");
        }
        if (request.getStoreId() == null) {
            throw new IllegalArgumentException("storeId is required");
        }
        if (request.getUserLat() == null || request.getUserLng() == null) {
            throw new IllegalArgumentException("location is required");
        }
    }

    private Store getRequiredStore(Long storeId) {
        Store store = storeService.getById(storeId);
        if (store == null) {
            throw new IllegalArgumentException("store not found");
        }
        if (store.getLatitude() == null || store.getLongitude() == null) {
            throw new IllegalArgumentException("store location is missing");
        }
        return store;
    }

    private WashQueue findWaitingQueue(Long userId, Long storeId) {
        if (userId == null || storeId == null) {
            return null;
        }
        return this.getOne(
            new LambdaQueryWrapper<WashQueue>()
                .eq(WashQueue::getUserId, userId)
                .eq(WashQueue::getStoreId, storeId)
                .eq(WashQueue::getQueueStatus, STATUS_WAITING)
                .orderByAsc(WashQueue::getId)
                .last("limit 1")
        );
    }

    private void updateQueueLocation(WashQueue queue, Double userLat, Double userLng, double distanceKm) {
        LambdaUpdateWrapper<WashQueue> wrapper = new LambdaUpdateWrapper<WashQueue>()
            .eq(WashQueue::getId, queue.getId())
            .eq(WashQueue::getQueueStatus, STATUS_WAITING)
            .set(WashQueue::getUserLatitude, BigDecimal.valueOf(userLat).setScale(6, RoundingMode.HALF_UP))
            .set(WashQueue::getUserLongitude, BigDecimal.valueOf(userLng).setScale(6, RoundingMode.HALF_UP))
            .set(WashQueue::getDistanceKm, BigDecimal.valueOf(distanceKm).setScale(3, RoundingMode.HALF_UP));
        this.update(wrapper);
    }

    private void cancelQueue(WashQueue queue, double distanceKm, String reason) {
        LambdaUpdateWrapper<WashQueue> wrapper = new LambdaUpdateWrapper<WashQueue>()
            .eq(WashQueue::getId, queue.getId())
            .eq(WashQueue::getQueueStatus, STATUS_WAITING)
            .set(WashQueue::getQueueStatus, STATUS_CANCELLED)
            .set(WashQueue::getDistanceKm, BigDecimal.valueOf(distanceKm).setScale(3, RoundingMode.HALF_UP))
            .set(WashQueue::getCancelReason, reason)
            .set(WashQueue::getCancelledAt, LocalDateTime.now());
        this.update(wrapper);
    }

    private Map<String, Object> buildQueueStatus(WashQueue queue, Double distanceKm) {
        if (queue == null) {
            return buildEmptyStatus(null, distanceKm);
        }
        long aheadCount = this.count(
            new LambdaQueryWrapper<WashQueue>()
                .eq(WashQueue::getStoreId, queue.getStoreId())
                .eq(WashQueue::getQueueStatus, STATUS_WAITING)
                .lt(WashQueue::getId, queue.getId())
        );
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("active", true);
        result.put("queueId", queue.getId());
        result.put("queueNo", queue.getQueueNo());
        result.put("storeId", queue.getStoreId());
        result.put("aheadCount", aheadCount);
        result.put("position", aheadCount + 1);
        result.put("distanceKm", distanceKm);
        result.put("cancelled", false);
        return result;
    }

    private Map<String, Object> buildEmptyStatus(Long storeId, Double distanceKm) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("active", false);
        result.put("queueId", null);
        result.put("queueNo", "");
        result.put("storeId", storeId);
        result.put("aheadCount", 0);
        result.put("position", 0);
        result.put("distanceKm", distanceKm);
        result.put("cancelled", false);
        return result;
    }

    private double resolveDistanceKm(Store store, Double userLat, Double userLng) {
        double distance = haversine(
            userLat,
            userLng,
            store.getLatitude().doubleValue(),
            store.getLongitude().doubleValue()
        );
        return Math.round(distance * 1000.0) / 1000.0;
    }

    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
            + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
            * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return 6371.0 * c;
    }
}
