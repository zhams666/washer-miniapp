package com.washer.backend.service;

import com.washer.backend.entity.PointRedemptionOrder;
import java.util.List;

public interface PointRedemptionService {

    PointRedemptionOrder redeem(Long userId, Long productId, String requestNo);

    List<PointRedemptionOrder> listByUser(Long userId, int limit);
}
