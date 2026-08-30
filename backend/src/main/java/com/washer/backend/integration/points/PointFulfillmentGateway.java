package com.washer.backend.integration.points;

import com.washer.backend.entity.PointRedemptionOrder;

/** Replace this contract with a vendor-specific fulfillment adapter for physical goods or coupons. */
public interface PointFulfillmentGateway {

    PointFulfillmentResult fulfill(PointRedemptionOrder order);
}
