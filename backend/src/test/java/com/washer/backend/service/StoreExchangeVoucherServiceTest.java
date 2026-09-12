package com.washer.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.washer.backend.dto.miniadmin.MiniAdminExchangeVoucherItem;
import com.washer.backend.entity.Store;
import com.washer.backend.entity.StoreExchangeVoucher;
import com.washer.backend.mapper.StoreExchangeVoucherMapper;
import com.washer.backend.mapper.StoreMapper;
import com.washer.backend.mapper.UserInfoMapper;
import com.washer.backend.mapper.UserStoreWalletMapper;
import com.washer.backend.mapper.WalletTransactionMapper;
import com.washer.backend.service.impl.StoreExchangeVoucherServiceImpl;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StoreExchangeVoucherServiceTest {

    @Mock
    private StoreExchangeVoucherMapper voucherMapper;

    @Mock
    private StoreMapper storeMapper;

    @Mock
    private UserInfoMapper userInfoMapper;

    @Mock
    private UserStoreWalletMapper userStoreWalletMapper;

    @Mock
    private WalletTransactionMapper walletTransactionMapper;

    @Mock
    private MiniAdminAuthService miniAdminAuthService;

    @InjectMocks
    private StoreExchangeVoucherServiceImpl voucherService;

    @Test
    void pageAdminVouchers_mapsUnusedVoucherWithoutRedeemReferences() {
        StoreExchangeVoucher voucher = new StoreExchangeVoucher();
        voucher.setId(1L);
        voucher.setBatchNo("EVB001");
        voucher.setSerialNo("EV001");
        voucher.setStoreId(10L);
        voucher.setAmount(new BigDecimal("20.00"));
        voucher.setStatus("unused");

        Page<StoreExchangeVoucher> source = new Page<>(1, 10, 1);
        source.setRecords(List.of(voucher));
        Store store = new Store();
        store.setId(10L);
        store.setStoreName("测试门店");
        when(voucherMapper.selectPage(any(), any())).thenReturn(source);
        when(storeMapper.selectBatchIds(List.of(10L))).thenReturn(List.of(store));

        Page<MiniAdminExchangeVoucherItem> result = voucherService.pageAdminVouchers(1, 10, null, null, null);

        assertThat(result.getRecords()).singleElement().satisfies(item -> {
            assertThat(item.getStatus()).isEqualTo("unused");
            assertThat(item.getRedeemedUserId()).isNull();
            assertThat(item.getRedeemedUserNickname()).isEmpty();
            assertThat(item.getRedeemTransactionNo()).isEmpty();
        });
    }
}
