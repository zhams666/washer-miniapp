package com.washer.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.washer.backend.dto.device.DeviceSimpleItem;
import com.washer.backend.entity.Device;
import com.washer.backend.mapper.DeviceMapper;
import com.washer.backend.service.DeviceService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DeviceServiceImpl extends ServiceImpl<DeviceMapper, Device> implements DeviceService {

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

        return this.list(wrapper).stream()
            .map(this::toSimpleItem)
            .toList();
    }

    @Override
    public DeviceSimpleItem getSimpleDeviceById(Long id) {
        Device device = this.getById(id);
        if (device == null) {
            return null;
        }
        return toSimpleItem(device);
    }

    private DeviceSimpleItem toSimpleItem(Device device) {
        return new DeviceSimpleItem(
            device.getId(),
            device.getDeviceCode(),
            device.getDeviceName(),
            device.getStoreId(),
            device.getDeviceStatus()
        );
    }
}
