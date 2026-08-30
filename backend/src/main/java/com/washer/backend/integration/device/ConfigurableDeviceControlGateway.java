package com.washer.backend.integration.device;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.washer.backend.config.DeviceGatewayProperties;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ConfigurableDeviceControlGateway implements DeviceControlGateway {

    private final DeviceGatewayProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public ConfigurableDeviceControlGateway(DeviceGatewayProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public DeviceCommandResult start(DeviceCommand command) {
        return dispatch(command, properties.getStartPath());
    }

    @Override
    public DeviceCommandResult stop(DeviceCommand command) {
        return dispatch(command, properties.getStopPath());
    }

    private DeviceCommandResult dispatch(DeviceCommand command, String pathTemplate) {
        if ("simulated".equalsIgnoreCase(properties.getMode())) {
            return DeviceCommandResult.accepted("SIM-" + UUID.randomUUID().toString().replace("-", ""), "simulated device command accepted");
        }
        if (!"provider".equalsIgnoreCase(properties.getMode())) {
            return DeviceCommandResult.rejected("washer.device.mode must be simulated or provider");
        }
        if (!StringUtils.hasText(properties.getBaseUrl()) || !StringUtils.hasText(properties.getApiKey())) {
            return DeviceCommandResult.rejected("device provider is not configured");
        }
        try {
            String path = StringUtils.hasText(pathTemplate) ? pathTemplate : "/devices/{deviceCode}/" + command.action();
            path = path.replace("{deviceCode}", command.deviceCode());
            String body = objectMapper.writeValueAsString(Map.of(
                "deviceId", command.deviceId(), "deviceCode", command.deviceCode(), "storeId", command.storeId(), "action", command.action()
            ));
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(stripTrailingSlash(properties.getBaseUrl()) + path))
                .header("Content-Type", "application/json")
                .header(properties.getApiKeyHeader(), properties.getApiKey())
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return DeviceCommandResult.rejected("device provider HTTP " + response.statusCode());
            }
            JsonNode root = objectMapper.readTree(response.body());
            String actual = root.path(properties.getSuccessField()).asText();
            if (!properties.getSuccessValue().equals(actual)) {
                return DeviceCommandResult.rejected("device provider rejected command: " + response.body());
            }
            String commandNo = root.path("commandNo").asText("VENDOR-" + UUID.randomUUID().toString().replace("-", ""));
            return DeviceCommandResult.accepted(commandNo, "provider accepted");
        } catch (Exception ex) {
            return DeviceCommandResult.rejected("device provider request failed: " + ex.getMessage());
        }
    }

    private String stripTrailingSlash(String value) {
        String text = value.trim();
        return text.endsWith("/") ? text.substring(0, text.length() - 1) : text;
    }
}
