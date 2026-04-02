package com.washer.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.washer.backend.dto.device.DeviceSimpleItem;
import com.washer.backend.entity.Device;
import com.washer.backend.entity.Store;
import com.washer.backend.mapper.DeviceMapper;
import com.washer.backend.service.DeviceService;
import com.washer.backend.service.StoreService;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DeviceServiceImpl extends ServiceImpl<DeviceMapper, Device> implements DeviceService {

    private final StoreService storeService;

    public DeviceServiceImpl(StoreService storeService) {
        this.storeService = storeService;
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
