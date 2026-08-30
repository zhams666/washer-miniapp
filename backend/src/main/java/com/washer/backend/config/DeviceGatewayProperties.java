package com.washer.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "washer.device")
public class DeviceGatewayProperties {

    private String mode = "simulated";
    private String vendor = "unconfigured";
    private String baseUrl;
    private String startPath = "/devices/{deviceCode}/start";
    private String stopPath = "/devices/{deviceCode}/stop";
    private String apiKeyHeader = "X-Api-Key";
    private String apiKey;
    private String successField = "code";
    private String successValue = "0";

    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
    public String getVendor() { return vendor; }
    public void setVendor(String vendor) { this.vendor = vendor; }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getStartPath() { return startPath; }
    public void setStartPath(String startPath) { this.startPath = startPath; }
    public String getStopPath() { return stopPath; }
    public void setStopPath(String stopPath) { this.stopPath = stopPath; }
    public String getApiKeyHeader() { return apiKeyHeader; }
    public void setApiKeyHeader(String apiKeyHeader) { this.apiKeyHeader = apiKeyHeader; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getSuccessField() { return successField; }
    public void setSuccessField(String successField) { this.successField = successField; }
    public String getSuccessValue() { return successValue; }
    public void setSuccessValue(String successValue) { this.successValue = successValue; }
}
