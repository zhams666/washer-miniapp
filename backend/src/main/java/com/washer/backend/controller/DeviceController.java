package com.washer.backend.controller;

import com.washer.backend.common.ApiResponse;
import com.washer.backend.dto.device.DeviceSimpleItem;
import com.washer.backend.entity.Device;
import com.washer.backend.service.DeviceService;
import com.washer.backend.service.WashOrderService;
import java.util.List;
import java.util.UUID;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/devices")
public class DeviceController {

    private final DeviceService deviceService;
    private final WashOrderService washOrderService;

    public DeviceController(DeviceService deviceService, WashOrderService washOrderService) {
        this.deviceService = deviceService;
        this.washOrderService = washOrderService;
    }

    @GetMapping
    public ApiResponse<List<DeviceSimpleItem>> list(
        @RequestParam(required = false) Long storeId,
        @RequestParam(required = false) String keyword
    ) {
        return ApiResponse.success(deviceService.getSimpleDevices(storeId, keyword));
    }

    @GetMapping("/{id}")
    public ApiResponse<DeviceSimpleItem> getById(@PathVariable Long id) {
        DeviceSimpleItem device = deviceService.getSimpleDeviceById(id);
        if (device == null) {
            throw new IllegalArgumentException("device not found");
        }
        return ApiResponse.success(device);
    }

    @PostMapping
    public ApiResponse<Device> create(@RequestBody Device device) {
        if (!StringUtils.hasText(device.getDeviceCode())) {
            device.setDeviceCode("D" + UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        }
        if (device.getStoreId() == null) {
            throw new IllegalArgumentException("storeId is required");
        }
        if (!StringUtils.hasText(device.getDeviceType())) {
            device.setDeviceType("washer");
        }
        if (!StringUtils.hasText(device.getDeviceRole())) {
            device.setDeviceRole("main");
        }
        if (!StringUtils.hasText(device.getDeviceStatus())) {
            device.setDeviceStatus("offline");
        }
        deviceService.save(device);
        return ApiResponse.success("created", device);
    }

    @PostMapping({"/{id}/start", "/{id}/mock-start"})
    public ApiResponse<DeviceSimpleItem> mockStart(@PathVariable Long id) {
        return ApiResponse.success("started", deviceService.mockStartDevice(id));
    }

    @PostMapping({"/{id}/stop", "/{id}/mock-stop"})
    public ApiResponse<DeviceSimpleItem> mockStop(@PathVariable Long id) {
        washOrderService.cancelRunningOrdersForDevice(id, "管理员模拟停止设备");
        return ApiResponse.success("stopped", deviceService.mockStopDevice(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<Device> update(@PathVariable Long id, @RequestBody Device device) {
        device.setId(id);
        if (shouldStopRunningOrders(device.getDeviceStatus())) {
            washOrderService.cancelRunningOrdersForDevice(id, "管理员更新设备状态");
        }
        boolean updated = deviceService.updateById(device);
        if (!updated) {
            throw new IllegalArgumentException("device update failed");
        }
        return ApiResponse.success("updated", deviceService.getById(id));
    }

    private boolean shouldStopRunningOrders(String deviceStatus) {
        if (!StringUtils.hasText(deviceStatus)) {
            return false;
        }
        String normalizedStatus = deviceStatus.trim().toLowerCase();
        return !"running".equals(normalizedStatus) && !"online".equals(normalizedStatus);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Boolean> delete(@PathVariable Long id) {
        boolean removed = deviceService.removeById(id);
        if (!removed) {
            throw new IllegalArgumentException("device delete failed");
        }
        return ApiResponse.success("deleted", true);
    }
}
