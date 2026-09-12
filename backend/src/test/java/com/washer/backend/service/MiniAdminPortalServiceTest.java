package com.washer.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.washer.backend.dto.device.DeviceSimpleItem;
import com.washer.backend.dto.miniadmin.MiniAdminDeviceCreateRequest;
import com.washer.backend.dto.miniadmin.MiniAdminDeviceConfigRequest;
import com.washer.backend.dto.miniadmin.MiniAdminSessionContext;
import com.washer.backend.dto.miniadmin.MiniAdminStoreOption;
import com.washer.backend.entity.Device;
import com.washer.backend.entity.MiniAdminStaff;
import com.washer.backend.entity.Store;
import com.washer.backend.mapper.CardUsageRecordMapper;
import com.washer.backend.mapper.DeviceMapper;
import com.washer.backend.mapper.FranchiseeMapper;
import com.washer.backend.mapper.MiniAdminStaffMapper;
import com.washer.backend.mapper.RankingDisplayAdjustmentMapper;
import com.washer.backend.mapper.RechargeOrderMapper;
import com.washer.backend.mapper.StoreMapper;
import com.washer.backend.mapper.UserInfoMapper;
import com.washer.backend.mapper.WalletTransactionMapper;
import com.washer.backend.mapper.WashOrderMapper;
import com.washer.backend.service.impl.MiniAdminPortalServiceImpl;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MiniAdminPortalServiceTest {

    @Mock private StoreMapper storeMapper;
    @Mock private DeviceMapper deviceMapper;
    @Mock private WashOrderMapper washOrderMapper;
    @Mock private WalletTransactionMapper walletTransactionMapper;
    @Mock private RechargeOrderMapper rechargeOrderMapper;
    @Mock private RankingDisplayAdjustmentMapper rankingDisplayAdjustmentMapper;
    @Mock private CardUsageRecordMapper cardUsageRecordMapper;
    @Mock private MiniAdminStaffMapper miniAdminStaffMapper;
    @Mock private FranchiseeMapper franchiseeMapper;
    @Mock private UserInfoMapper userInfoMapper;
    @Mock private DeviceService deviceService;
    @Mock private RankingDisplayAdjustmentService rankingDisplayAdjustmentService;
    @Mock private WashOrderService washOrderService;

    @InjectMocks
    private MiniAdminPortalServiceImpl service;

    @Test
    void createDevice_rejectsStoreOutsideManagerScope() {
        MiniAdminSessionContext context = managerContext(10L);
        MiniAdminDeviceCreateRequest request = new MiniAdminDeviceCreateRequest();
        request.setStoreId(20L);

        assertThatThrownBy(() -> service.createDevice(context, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("无权访问该门店");
        verifyNoInteractions(deviceService);
    }

    @Test
    void createDevice_delegatesAuthorizedStoreToSharedDeviceService() {
        MiniAdminSessionContext context = managerContext(10L);
        MiniAdminDeviceCreateRequest request = new MiniAdminDeviceCreateRequest();
        request.setStoreId(10L);
        request.setDeviceName("手机新增设备");
        request.setDeviceType("washer");
        Store store = new Store();
        store.setId(10L);
        when(storeMapper.selectById(10L)).thenReturn(store);
        when(deviceService.createManagedDevice(any(Device.class))).thenAnswer(invocation -> {
            Device device = invocation.getArgument(0, Device.class);
            device.setId(88L);
            return device;
        });
        DeviceSimpleItem expected = new DeviceSimpleItem();
        when(deviceService.getSimpleDeviceById(88L)).thenReturn(expected);

        DeviceSimpleItem result = service.createDevice(context, request);

        assertThat(result).isSameAs(expected);
        verify(deviceService).createManagedDevice(any(Device.class));
        verify(deviceService).getSimpleDeviceById(88L);
    }

    @Test
    void updateDeviceConfig_cancelsRunningOrderWhenStatusBecomesIdle() {
        MiniAdminSessionContext context = managerContext(10L);
        Device device = new Device();
        device.setId(88L);
        device.setStoreId(10L);
        device.setDeviceStatus("running");
        when(deviceMapper.selectById(88L)).thenReturn(device);
        MiniAdminDeviceConfigRequest request = new MiniAdminDeviceConfigRequest();
        request.setDeviceCode("D88");
        request.setDeviceName("测试设备");
        request.setDeviceStatus("idle");
        DeviceSimpleItem expected = new DeviceSimpleItem();
        when(deviceService.updateMiniAdminConfig(88L, request)).thenReturn(expected);

        DeviceSimpleItem result = service.updateDeviceConfig(context, 88L, request);

        assertThat(result).isSameAs(expected);
        verify(washOrderService).cancelRunningOrdersForDevice(88L, "管理端更新设备状态");
        verify(deviceService).updateMiniAdminConfig(88L, request);
    }

    private MiniAdminSessionContext managerContext(Long storeId) {
        MiniAdminStaff staff = new MiniAdminStaff();
        staff.setId(1L);
        staff.setRoleCode("store_manager");
        return new MiniAdminSessionContext(
            staff,
            false,
            List.of(new MiniAdminStoreOption(storeId, null, "测试门店")),
            List.of("device:control", "store:edit")
        );
    }
}
