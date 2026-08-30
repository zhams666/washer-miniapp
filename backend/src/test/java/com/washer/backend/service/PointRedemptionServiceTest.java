package com.washer.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.washer.backend.entity.PointRedemptionOrder;
import com.washer.backend.integration.points.PointFulfillmentGateway;
import com.washer.backend.mapper.PointMallProductMapper;
import com.washer.backend.mapper.PointRedemptionOrderMapper;
import com.washer.backend.mapper.UserInfoMapper;
import com.washer.backend.service.impl.PointRedemptionServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class PointRedemptionServiceTest {

    @Mock private PointMallProductMapper productMapper;
    @Mock private PointRedemptionOrderMapper orderMapper;
    @Mock private UserInfoMapper userInfoMapper;
    @Mock private PointFulfillmentGateway fulfillmentGateway;
    @Mock private JdbcTemplate jdbcTemplate;
    @InjectMocks private PointRedemptionServiceImpl service;

    @Test
    void redeem_returnsExistingOrderForSameUserRequestNumber() {
        PointRedemptionOrder existing = new PointRedemptionOrder();
        existing.setRedemptionNo("PR001");
        existing.setRequestNo("REQUEST-1");
        when(orderMapper.selectOne(any())).thenReturn(existing);

        PointRedemptionOrder result = service.redeem(1L, 2L, "REQUEST-1");

        assertThat(result).isSameAs(existing);
        verifyNoInteractions(productMapper, userInfoMapper, fulfillmentGateway, jdbcTemplate);
    }
}
