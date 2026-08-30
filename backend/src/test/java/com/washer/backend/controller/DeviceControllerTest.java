package com.washer.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.washer.backend.dto.device.DeviceSimpleItem;
import com.washer.backend.entity.Device;
import com.washer.backend.service.DeviceService;
import com.washer.backend.service.WashOrderService;
import com.washer.backend.integration.device.DeviceCommandResult;
import com.washer.backend.integration.device.DeviceControlGateway;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeviceControllerTest {

    @Mock
    private DeviceService deviceService;

    @Mock
    private WashOrderService washOrderService;

    @Mock
    private DeviceControlGateway deviceControlGateway;

    @InjectMocks
    private DeviceController deviceController;

    @Test
    void mockStop_cancelsRunningOrdersBeforeMarkingDeviceIdle() {
        DeviceSimpleItem stoppedDevice = deviceItem(1L, "idle");
        when(deviceService.mockStopDevice(1L)).thenReturn(stoppedDevice);

        DeviceSimpleItem result = deviceController.mockStop(1L).getData();

        assertThat(result).isSameAs(stoppedDevice);
        InOrder orderedCalls = inOrder(washOrderService, deviceService);
        orderedCalls.verify(washOrderService).cancelRunningOrdersForDevice(1L, "管理员模拟停止设备");
        orderedCalls.verify(deviceService).mockStopDevice(1L);
    }

    @Test
    void update_idleDevice_cancelsRunningOrders() {
        Device device = new Device();
        device.setDeviceStatus("idle");
        when(deviceService.updateById(device)).thenReturn(true);
        when(deviceService.getById(1L)).thenReturn(device);

        deviceController.update(1L, device);

        verify(washOrderService).cancelRunningOrdersForDevice(1L, "管理员更新设备状态");
        verify(deviceService).updateById(device);
    }

    private DeviceSimpleItem deviceItem(Long id, String status) {
        return new DeviceSimpleItem(
            id,
            "S001",
            "工位1",
            1L,
            "门店1",
            "washer",
            "main",
            status,
            status,
            "",
            "",
            "",
            LocalDateTime.now(),
            LocalDateTime.now()
        );
    }
}
