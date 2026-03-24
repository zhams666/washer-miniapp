package com.washer.backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.washer.backend.common.ApiResponse;
import com.washer.backend.entity.Device;
import com.washer.backend.service.DeviceService;
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

    public DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @GetMapping
    public ApiResponse<Page<Device>> page(
        @RequestParam(defaultValue = "1") long page,
        @RequestParam(defaultValue = "10") long size,
        @RequestParam(required = false) Long storeId,
        @RequestParam(required = false) Long bayId,
        @RequestParam(required = false) String deviceStatus,
        @RequestParam(required = false) String keyword
    ) {
        LambdaQueryWrapper<Device> wrapper = new LambdaQueryWrapper<Device>()
            .eq(storeId != null, Device::getStoreId, storeId)
            .eq(bayId != null, Device::getBayId, bayId)
            .eq(StringUtils.hasText(deviceStatus), Device::getDeviceStatus, deviceStatus)
            .orderByDesc(Device::getId);

        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w
                .like(Device::getDeviceCode, keyword)
                .or()
                .like(Device::getDeviceName, keyword));
        }

        return ApiResponse.success(deviceService.page(new Page<>(page, size), wrapper));
    }

    @GetMapping("/{id}")
    public ApiResponse<Device> getById(@PathVariable Long id) {
        Device device = deviceService.getById(id);
        if (device == null) {
            throw new IllegalArgumentException("设备不存在");
        }
        return ApiResponse.success(device);
    }

    @PostMapping
    public ApiResponse<Device> create(@RequestBody Device device) {
        if (!StringUtils.hasText(device.getDeviceCode())) {
            device.setDeviceCode("D" + UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        }
        if (device.getStoreId() == null) {
            throw new IllegalArgumentException("storeId 不能为空");
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
        return ApiResponse.success("创建成功", device);
    }

    @PutMapping("/{id}")
    public ApiResponse<Device> update(@PathVariable Long id, @RequestBody Device device) {
        device.setId(id);
        boolean updated = deviceService.updateById(device);
        if (!updated) {
            throw new IllegalArgumentException("设备不存在或更新失败");
        }
        return ApiResponse.success("更新成功", deviceService.getById(id));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Boolean> delete(@PathVariable Long id) {
        boolean removed = deviceService.removeById(id);
        if (!removed) {
            throw new IllegalArgumentException("设备不存在或删除失败");
        }
        return ApiResponse.success("删除成功", true);
    }
}
