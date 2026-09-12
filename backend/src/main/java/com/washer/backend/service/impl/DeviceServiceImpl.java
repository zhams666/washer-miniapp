package com.washer.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.washer.backend.dto.device.DeviceSimpleItem;
import com.washer.backend.dto.miniadmin.MiniAdminDeviceConfigRequest;
import com.washer.backend.entity.Device;
import com.washer.backend.integration.device.DeviceCommand;
import com.washer.backend.integration.device.DeviceCommandResult;
import com.washer.backend.integration.device.DeviceControlGateway;
import com.washer.backend.entity.Store;
import com.washer.backend.mapper.DeviceMapper;
import com.washer.backend.service.DeviceService;
import com.washer.backend.service.StoreService;
import com.washer.backend.support.DeviceManagementRemark;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
    public Device createManagedDevice(Device device) {
        if (device == null) {
            throw new IllegalArgumentException("device is required");
        }
        if (device.getStoreId() == null || device.getStoreId() <= 0) {
            throw new IllegalArgumentException("storeId is required");
        }
        if (storeService.getById(device.getStoreId()) == null) {
            throw new IllegalArgumentException("store not found");
        }

        String deviceCode = limitText(device.getDeviceCode(), 64);
        if (!StringUtils.hasText(deviceCode)) {
            deviceCode = "D" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        }
        device.setDeviceCode(deviceCode);
        device.setDeviceName(limitText(device.getDeviceName(), 100));
        device.setDeviceType(defaultText(device.getDeviceType(), "washer", 40));
        device.setDeviceRole(defaultText(device.getDeviceRole(), "main", 40));
        device.setDeviceStatus(defaultText(device.getDeviceStatus(), "offline", 20).toLowerCase());
        device.setProtocolType(limitText(device.getProtocolType(), 80));
        device.setFirmwareVersion(limitText(device.getFirmwareVersion(), 80));
        device.setRemark(limitText(device.getRemark(), 1000));

        if (!this.save(device)) {
            throw new IllegalStateException("device create failed");
        }
        return device;
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DeviceSimpleItem updateMiniAdminConfig(Long id, MiniAdminDeviceConfigRequest request) {
        Device device = getRequiredDevice(id);
        if (request == null) {
            throw new IllegalArgumentException("request is required");
        }
        String deviceCode = normalizeText(request.getDeviceCode());
        if (!StringUtils.hasText(deviceCode)) {
            throw new IllegalArgumentException("deviceCode is required");
        }
        String deviceName = normalizeText(request.getDeviceName());
        if (!StringUtils.hasText(deviceName)) {
            throw new IllegalArgumentException("deviceName is required");
        }

        Map<String, Object> config = DeviceManagementRemark.parse(device.getRemark());
        put(config, "baseTimeMinutes", normalizeInteger(request.getBaseTimeMinutes(), 0, 24 * 60));
        put(config, "basePrice", normalizeAmount(request.getBasePrice()));
        put(config, "overtimePrice", normalizeAmount(request.getOvertimePrice()));
        put(config, "speakerSn", limitText(request.getSpeakerSn(), 64));
        put(config, "speakerVersion", limitText(request.getSpeakerVersion(), 50));
        put(config, "cabinetName", limitText(request.getCabinetName(), 80));

        Device update = new Device();
        update.setId(device.getId());
        update.setDeviceCode(deviceCode);
        update.setDeviceName(deviceName);
        String deviceStatus = normalizeText(request.getDeviceStatus()).toLowerCase();
        if (StringUtils.hasText(deviceStatus)) {
            update.setDeviceStatus(normalizeManagedStatus(deviceStatus));
        }
        update.setFirmwareVersion(limitText(request.getSpeakerVersion(), 50));
        update.setRemark(DeviceManagementRemark.serialize(config));
        if (!this.updateById(update)) {
            throw new IllegalArgumentException("device update failed");
        }
        return getSimpleDeviceById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DeviceSimpleItem applyManagementAction(Long id, String action) {
        Device device = getRequiredDevice(id);
        String normalizedAction = normalizeAction(action);
        Map<String, Object> config = DeviceManagementRemark.parse(device.getRemark());
        Device update = new Device();
        update.setId(device.getId());
        LocalDateTime now = LocalDateTime.now();
        put(config, "lastAction", normalizedAction);
        put(config, "lastActionAt", now.toString());

        switch (normalizedAction) {
            case "open_door" -> put(config, "doorState", "open");
            case "close_door" -> put(config, "doorState", "closed");
            case "power_on" -> {
                put(config, "powerState", "on");
                put(config, "maintenanceMode", false);
                update.setDeviceStatus(STATUS_IDLE);
                update.setLastHeartbeatTime(now);
                update.setLastOnlineTime(now);
            }
            case "power_off" -> {
                put(config, "powerState", "off");
                update.setDeviceStatus("offline");
                update.setLastHeartbeatTime(now);
            }
            case "maintenance" -> {
                put(config, "maintenanceMode", true);
                put(config, "powerState", "on");
                update.setDeviceStatus("paused");
                update.setLastHeartbeatTime(now);
            }
            default -> throw new IllegalArgumentException("unsupported device action");
        }

        update.setRemark(DeviceManagementRemark.serialize(config));
        if (!this.updateById(update)) {
            throw new IllegalArgumentException("device update failed");
        }
        return getSimpleDeviceById(id);
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

    private String normalizeManagedStatus(String value) {
        return switch (value) {
            case "offline", "idle", "running", "paused", "fault", "disabled" -> value;
            default -> throw new IllegalArgumentException("unsupported device status");
        };
    }

    private DeviceSimpleItem toSimpleItem(Device device, Map<Long, Store> storeMap) {
        Store store = storeMap.get(device.getStoreId());
        Map<String, Object> config = DeviceManagementRemark.parse(device.getRemark());

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
            resolveAgentLevel(store),
            store != null ? store.getContactName() : "",
            store != null ? store.getContactPhone() : "",
            DeviceManagementRemark.integer(config, "baseTimeMinutes"),
            DeviceManagementRemark.decimal(config, "basePrice"),
            DeviceManagementRemark.decimal(config, "overtimePrice"),
            DeviceManagementRemark.text(config, "speakerSn"),
            StringUtils.hasText(DeviceManagementRemark.text(config, "speakerVersion"))
                ? DeviceManagementRemark.text(config, "speakerVersion")
                : device.getFirmwareVersion(),
            DeviceManagementRemark.text(config, "cabinetName"),
            DeviceManagementRemark.text(config, "doorState"),
            DeviceManagementRemark.text(config, "powerState"),
            Boolean.TRUE.equals(DeviceManagementRemark.bool(config, "maintenanceMode")),
            device.getCreatedAt(),
            device.getUpdatedAt()
        );
    }

    private String resolveAgentLevel(Store store) {
        if (store == null || store.getFranchiseeId() == null || store.getFranchiseeId() <= 0) {
            return "直营";
        }
        return "1级代理";
    }

    private String normalizeAction(String action) {
        String value = normalizeText(action).replace("-", "_").toLowerCase();
        return switch (value) {
            case "opendoor", "open_door" -> "open_door";
            case "closedoor", "close_door" -> "close_door";
            case "poweron", "power_on" -> "power_on";
            case "poweroff", "power_off" -> "power_off";
            case "maintenance", "maintain" -> "maintenance";
            default -> value;
        };
    }

    private String normalizeText(String value) {
        return value != null ? value.trim() : "";
    }

    private String limitText(String value, int maxLength) {
        String text = normalizeText(value);
        return text.length() <= maxLength ? text : text.substring(0, maxLength);
    }

    private String defaultText(String value, String fallback, int maxLength) {
        String text = limitText(value, maxLength);
        return StringUtils.hasText(text) ? text : fallback;
    }

    private Integer normalizeInteger(Integer value, int min, int max) {
        if (value == null) {
            return null;
        }
        return Math.max(min, Math.min(value, max));
    }

    private BigDecimal normalizeAmount(BigDecimal value) {
        if (value == null) {
            return null;
        }
        BigDecimal amount = value.setScale(2, RoundingMode.HALF_UP);
        return amount.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : amount;
    }

    private void put(Map<String, Object> config, String key, Object value) {
        if (config == null) {
            return;
        }
        if (value == null) {
            config.remove(key);
            return;
        }
        config.put(key, value);
    }
}
