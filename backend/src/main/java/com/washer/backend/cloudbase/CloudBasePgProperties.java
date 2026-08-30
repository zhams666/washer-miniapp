package com.washer.backend.cloudbase;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "cloudbase.pg")
public class CloudBasePgProperties {

    @NotBlank(message = "CLOUDBASE_ENV_ID is required when SPRING_PROFILES_ACTIVE=cloudbase")
    @Pattern(regexp = "[a-zA-Z0-9-]+", message = "CLOUDBASE_ENV_ID contains unsupported characters")
    private String envId;

    @NotBlank(message = "CLOUDBASE_API_KEY is required when SPRING_PROFILES_ACTIVE=cloudbase")
    private String apiKey;

    private String gatewayBaseUrl;

    public String getEnvId() {
        return envId;
    }

    public void setEnvId(String envId) {
        this.envId = envId;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getGatewayBaseUrl() {
        if (gatewayBaseUrl == null || gatewayBaseUrl.isBlank()) {
            return "https://" + envId + ".api.tcloudbasegateway.com";
        }
        return gatewayBaseUrl;
    }

    public void setGatewayBaseUrl(String gatewayBaseUrl) {
        this.gatewayBaseUrl = gatewayBaseUrl;
    }
}
