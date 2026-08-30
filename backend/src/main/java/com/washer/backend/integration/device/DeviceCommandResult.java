package com.washer.backend.integration.device;

public record DeviceCommandResult(boolean accepted, String providerCommandNo, String message) {

    public static DeviceCommandResult accepted(String commandNo, String message) {
        return new DeviceCommandResult(true, commandNo, message);
    }

    public static DeviceCommandResult rejected(String message) {
        return new DeviceCommandResult(false, null, message);
    }
}
