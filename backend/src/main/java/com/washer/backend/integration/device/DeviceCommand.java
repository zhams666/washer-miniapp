package com.washer.backend.integration.device;

import com.washer.backend.entity.Device;

public record DeviceCommand(Long deviceId, String deviceCode, Long storeId, String action) {

    public static DeviceCommand start(Device device) {
        return from(device, "start");
    }

    public static DeviceCommand stop(Device device) {
        return from(device, "stop");
    }

    private static DeviceCommand from(Device device, String action) {
        if (device == null || device.getId() == null || device.getDeviceCode() == null) {
            throw new IllegalArgumentException("device command requires id and deviceCode");
        }
        return new DeviceCommand(device.getId(), device.getDeviceCode(), device.getStoreId(), action);
    }
}
