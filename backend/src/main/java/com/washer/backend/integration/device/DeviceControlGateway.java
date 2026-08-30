package com.washer.backend.integration.device;

/** Implement this interface for a specific washer vendor protocol. */
public interface DeviceControlGateway {

    DeviceCommandResult start(DeviceCommand command);

    DeviceCommandResult stop(DeviceCommand command);
}
