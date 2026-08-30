package com.washer.backend.integration.commerce;

import com.washer.backend.config.CommerceProperties;
import com.washer.backend.entity.CardPurchaseOrder;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Default provider-mode adapter. Replace it with the payment provider integration. */
@Component
public class ConfigurableCardPaymentGateway implements CardPaymentGateway {

    private final CommerceProperties properties;

    public ConfigurableCardPaymentGateway(CommerceProperties properties) {
        this.properties = properties;
    }

    @Override
    public CardPaymentResult createPayment(CardPurchaseOrder order) {
        return CardPaymentResult.pending(order.getPurchaseOrderNo(), "card payment provider " + properties.getProviderName() + " is not configured");
    }

    @Override
    public CardPaymentResult verifyCallback(CardPurchaseOrder order, Map<String, Object> payload) {
        return new CardPaymentResult("rejected", null, "card payment callback verification is not configured");
    }
}
