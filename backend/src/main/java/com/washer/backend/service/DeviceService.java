package com.washer.backend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.washer.backend.dto.device.DeviceSimpleItem;
import com.washer.backend.entity.Device;
import java.util.List;

public interface DeviceService extends IService<Device> {

    List<DeviceSimpleItem> getSimpleDevices(Long storeId, String keyword);

    DeviceSimpleItem getSimpleDeviceById(Long id);
}
