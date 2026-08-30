package com.washer.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "washer.commerce")
public class CommerceProperties {

    private String mode = "provider";
    private String providerName = "unconfigured";

    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
    public String getProviderName() { return providerName; }
    public void setProviderName(String providerName) { this.providerName = providerName; }
    public boolean isSimulationEnabled() { return "simulation".equalsIgnoreCase(mode); }
}
