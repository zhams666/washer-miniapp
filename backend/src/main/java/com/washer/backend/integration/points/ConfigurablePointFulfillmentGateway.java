package com.washer.backend.integration.points;

import com.washer.backend.config.PointMallProperties;
import com.washer.backend.entity.PointRedemptionOrder;
import org.springframework.stereotype.Component;

@Component
public class ConfigurablePointFulfillmentGateway implements PointFulfillmentGateway {

    private final PointMallProperties properties;

    public ConfigurablePointFulfillmentGateway(PointMallProperties properties) {
        this.properties = properties;
    }

    @Override
    public PointFulfillmentResult fulfill(PointRedemptionOrder order) {
        if (properties.isSimulationEnabled()) {
            return PointFulfillmentResult.completed("SIM-" + order.getRedemptionNo());
        }
        return PointFulfillmentResult.pending("point fulfillment provider is not configured");
    }
}
