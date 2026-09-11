package com.washer.backend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.washer.backend.dto.device.DeviceSimpleItem;
import com.washer.backend.dto.miniadmin.MiniAdminDeviceConfigRequest;
import com.washer.backend.entity.Device;
import java.util.List;

public interface DeviceService extends IService<Device> {

    List<DeviceSimpleItem> getSimpleDevices(Long storeId, String keyword);

    DeviceSimpleItem getSimpleDeviceById(Long id);

    DeviceSimpleItem mockStartDevice(Long id);

    DeviceSimpleItem mockStopDevice(Long id);

    DeviceSimpleItem updateMiniAdminConfig(Long id, MiniAdminDeviceConfigRequest request);

    DeviceSimpleItem applyManagementAction(Long id, String action);
}
