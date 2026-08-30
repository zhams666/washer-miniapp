package com.washer.backend.integration.commerce;

import static org.assertj.core.api.Assertions.assertThat;

import com.washer.backend.config.CommerceProperties;
import com.washer.backend.entity.CardPurchaseOrder;
import org.junit.jupiter.api.Test;

class ConfigurableCardPaymentGatewayTest {

    @Test
    void providerDefaultKeepsOrderPending() {
        CommerceProperties properties = new CommerceProperties();
        properties.setProviderName("vendor-x");
        CardPurchaseOrder order = new CardPurchaseOrder();
        order.setPurchaseOrderNo("CP001");

        CardPaymentResult result = new ConfigurableCardPaymentGateway(properties).createPayment(order);

        assertThat(result.status()).isEqualTo("pending");
        assertThat(result.providerOrderNo()).isEqualTo("CP001");
    }
}
