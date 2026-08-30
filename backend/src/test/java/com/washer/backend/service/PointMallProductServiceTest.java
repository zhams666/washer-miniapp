package com.washer.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.washer.backend.entity.PointMallProduct;
import com.washer.backend.mapper.PointMallProductMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PointMallProductServiceTest {

    @Mock
    private PointMallProductMapper productMapper;

    @InjectMocks
    private PointMallProductService pointMallProductService;

    @Test
    void saveProduct_rejectsPublishedProductWithNoStock() {
        Map<String, Object> payload = Map.of(
            "title", "洗车抵扣券",
            "productType", "coupon",
            "pointsPrice", 100,
            "stockTotal", 0,
            "status", 1
        );

        assertThatThrownBy(() -> pointMallProductService.saveProduct(null, payload))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("库存为 0 的商品不能上架");
    }

    @Test
    void changeStatus_publishesProductWithAvailableStock() {
        PointMallProduct product = new PointMallProduct();
        product.setId(8L);
        product.setStockTotal(3);
        product.setStatus(0);
        when(productMapper.selectById(8L)).thenReturn(product);

        PointMallProduct result = pointMallProductService.changeStatus(8L, 1);

        assertThat(result.getStatus()).isEqualTo(1);
        verify(productMapper).updateById(product);
    }
}
