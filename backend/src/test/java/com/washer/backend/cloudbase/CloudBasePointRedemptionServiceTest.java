package com.washer.backend.cloudbase;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.washer.backend.entity.PointRedemptionOrder;
import com.washer.backend.integration.points.PointFulfillmentGateway;
import com.washer.backend.integration.points.PointFulfillmentResult;
import com.washer.backend.mapper.PointRedemptionOrderMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CloudBasePointRedemptionServiceTest {

    @Test
    void redeemUsesOneRpcBeforeFulfillmentAndUpdatesTheReturnedOrder() throws Exception {
        CloudBasePgClient client = mock(CloudBasePgClient.class);
        PointRedemptionOrderMapper orderMapper = mock(PointRedemptionOrderMapper.class);
        PointFulfillmentGateway fulfillmentGateway = mock(PointFulfillmentGateway.class);
        PointRedemptionOrder order = new PointRedemptionOrder();
        order.setId(42L);
        order.setFulfillmentStatus("pending");
        when(client.rpc(eq("redeem_points_product"), any())).thenReturn(
            new ObjectMapper().readTree("[{\"id\":42}]")
        );
        when(orderMapper.selectById(42L)).thenReturn(order);
        when(fulfillmentGateway.fulfill(order)).thenReturn(PointFulfillmentResult.completed("coupon-42"));

        CloudBasePointRedemptionService service = new CloudBasePointRedemptionService(
            client, orderMapper, fulfillmentGateway
        );

        PointRedemptionOrder result = service.redeem(7L, 9L, "request-1");

        assertEquals("completed", result.getFulfillmentStatus());
        assertEquals("coupon-42", result.getFulfillmentReference());
        verify(client).rpc("redeem_points_product", Map.of(
            "p_user_id", 7L, "p_product_id", 9L, "p_request_no", "request-1"
        ));
        verify(orderMapper).updateById(order);
    }
}
