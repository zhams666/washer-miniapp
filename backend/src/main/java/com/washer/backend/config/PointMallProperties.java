package com.washer.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "washer.point-mall")
public class PointMallProperties {

    private String fulfillmentMode = "simulation";

    public String getFulfillmentMode() { return fulfillmentMode; }
    public void setFulfillmentMode(String fulfillmentMode) { this.fulfillmentMode = fulfillmentMode; }
    public boolean isSimulationEnabled() { return "simulation".equalsIgnoreCase(fulfillmentMode); }
}
