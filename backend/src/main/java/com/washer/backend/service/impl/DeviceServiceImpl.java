package com.washer.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.washer.backend.dto.device.DeviceSimpleItem;
import com.washer.backend.entity.Device;
import com.washer.backend.integration.device.DeviceCommand;
import com.washer.backend.integration.device.DeviceCommandResult;
import com.washer.backend.integration.device.DeviceControlGateway;
import com.washer.backend.entity.Store;
import com.washer.backend.mapper.DeviceMapper;
import com.washer.backend.service.DeviceService;
import com.washer.backend.service.StoreService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class DeviceServiceImpl extends ServiceImpl<DeviceMapper, Device> implements DeviceService {

    private static final String STATUS_RUNNING = "running";
    private static final String STATUS_IDLE = "idle";
    private static final String STATUS_FAULT = "fault";
    private static final String STATUS_DISABLED = "disabled";

    private final StoreService storeService;
    private final DeviceControlGateway deviceControlGateway;

    public DeviceServiceImpl(StoreService storeService, DeviceControlGateway deviceControlGateway) {
        this.storeService = storeService;
        this.deviceControlGateway = deviceControlGateway;
    }

    @Override
    public List<DeviceSimpleItem> getSimpleDevices(Long storeId, String keyword) {
        LambdaQueryWrapper<Device> wrapper = new LambdaQueryWrapper<Device>()
            .eq(storeId != null, Device::getStoreId, storeId)
            .orderByDesc(Device::getId);

        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w
                .like(Device::getDeviceCode, keyword)
                .or()
                .like(Device::getDeviceName, keyword));
        }

        List<Device> devices = this.list(wrapper);
        Map<Long, Store> storeMap = buildStoreMap(devices);

        return devices.stream()
            .map(device -> toSimpleItem(device, storeMap))
            .toList();
    }

    @Override
    public DeviceSimpleItem getSimpleDeviceById(Long id) {
        Device device = this.getById(id);
        if (device == null) {
            return null;
        }
        return toSimpleItem(device, buildStoreMap(List.of(device)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DeviceSimpleItem mockStartDevice(Long id) {
        Device device = getRequiredDevice(id);
        String currentStatus = normalizeDeviceStatus(device.getDeviceStatus());
        if (STATUS_RUNNING.equals(currentStatus)) {
            return toSimpleItem(device, buildStoreMap(List.of(device)));
        }
        if (STATUS_FAULT.equals(currentStatus) || STATUS_DISABLED.equals(currentStatus)) {
            throw new IllegalArgumentException("device is " + currentStatus);
        }
        DeviceCommandResult commandResult = deviceControlGateway.start(DeviceCommand.start(device));
        if (!commandResult.accepted()) {
            throw new IllegalStateException(commandResult.message());
        }

        LocalDateTime now = LocalDateTime.now();
        Device update = new Device();
        update.setId(device.getId());
        update.setDeviceStatus(STATUS_RUNNING);
        update.setLastHeartbeatTime(now);
        update.setLastOnlineTime(now);
        this.updateById(update);

        device.setDeviceStatus(STATUS_RUNNING);
        device.setLastHeartbeatTime(now);
        device.setLastOnlineTime(now);
        return toSimpleItem(device, buildStoreMap(List.of(device)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DeviceSimpleItem mockStopDevice(Long id) {
        Device device = getRequiredDevice(id);
        String currentStatus = normalizeDeviceStatus(device.getDeviceStatus());
        if (STATUS_IDLE.equals(currentStatus)) {
            return toSimpleItem(device, buildStoreMap(List.of(device)));
        }
        DeviceCommandResult commandResult = deviceControlGateway.stop(DeviceCommand.stop(device));
        if (!commandResult.accepted()) {
            throw new IllegalStateException(commandResult.message());
        }

        LocalDateTime now = LocalDateTime.now();
        Device update = new Device();
        update.setId(device.getId());
        update.setDeviceStatus(STATUS_IDLE);
        update.setLastHeartbeatTime(now);
        this.updateById(update);

        device.setDeviceStatus(STATUS_IDLE);
        device.setLastHeartbeatTime(now);
        return toSimpleItem(device, buildStoreMap(List.of(device)));
    }

    private Map<Long, Store> buildStoreMap(List<Device> devices) {
        List<Long> storeIds = devices.stream()
            .map(Device::getStoreId)
            .filter(storeId -> storeId != null)
            .distinct()
            .toList();

        if (storeIds.isEmpty()) {
            return Map.of();
        }

        return storeService.listByIds(storeIds).stream()
            .collect(Collectors.toMap(Store::getId, Function.identity(), (left, right) -> left));
    }

    private Device getRequiredDevice(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("device id is required");
        }
        Device device = this.getById(id);
        if (device == null) {
            throw new IllegalArgumentException("device not found");
        }
        return device;
    }

    private String normalizeDeviceStatus(String value) {
        return StringUtils.hasText(value) ? value.trim().toLowerCase() : "";
    }

    private DeviceSimpleItem toSimpleItem(Device device, Map<Long, Store> storeMap) {
        Store store = storeMap.get(device.getStoreId());

        return new DeviceSimpleItem(
            device.getId(),
            device.getDeviceCode(),
            device.getDeviceName(),
            device.getStoreId(),
            store != null ? store.getStoreName() : "",
            device.getDeviceType(),
            device.getDeviceRole(),
            device.getDeviceStatus(),
            device.getDeviceStatus(),
            device.getProtocolType(),
            device.getFirmwareVersion(),
            device.getRemark(),
            device.getCreatedAt(),
            device.getUpdatedAt()
        );
    }
}
