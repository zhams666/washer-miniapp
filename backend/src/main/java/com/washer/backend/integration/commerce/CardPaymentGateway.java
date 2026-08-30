package com.washer.backend.integration.commerce;

import com.washer.backend.entity.CardPurchaseOrder;
import java.util.Map;

/** Fill this adapter with the card-payment provider's prepay and callback verification API. */
public interface CardPaymentGateway {

    CardPaymentResult createPayment(CardPurchaseOrder order);

    CardPaymentResult verifyCallback(CardPurchaseOrder order, Map<String, Object> payload);
}
