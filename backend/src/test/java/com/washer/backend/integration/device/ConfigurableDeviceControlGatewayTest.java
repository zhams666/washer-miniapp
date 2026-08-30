package com.washer.backend.integration.device;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.washer.backend.config.DeviceGatewayProperties;
import com.washer.backend.entity.Device;
import org.junit.jupiter.api.Test;

class ConfigurableDeviceControlGatewayTest {

    @Test
    void simulatedModeAcceptsStartWithoutVendorConfiguration() {
        DeviceGatewayProperties properties = new DeviceGatewayProperties();
        properties.setMode("simulated");
        ConfigurableDeviceControlGateway gateway = new ConfigurableDeviceControlGateway(properties, new ObjectMapper());
        Device device = new Device();
        device.setId(1L);
        device.setStoreId(2L);
        device.setDeviceCode("D-01");

        DeviceCommandResult result = gateway.start(DeviceCommand.start(device));

        assertThat(result.accepted()).isTrue();
        assertThat(result.providerCommandNo()).startsWith("SIM-");
    }

    @Test
    void providerModeRejectsMissingVendorConfiguration() {
        DeviceGatewayProperties properties = new DeviceGatewayProperties();
        properties.setMode("provider");
        ConfigurableDeviceControlGateway gateway = new ConfigurableDeviceControlGateway(properties, new ObjectMapper());
        Device device = new Device();
        device.setId(1L);
        device.setDeviceCode("D-01");

        assertThat(gateway.stop(DeviceCommand.stop(device)).accepted()).isFalse();
    }
}
