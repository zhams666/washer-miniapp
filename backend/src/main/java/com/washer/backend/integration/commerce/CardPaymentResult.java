package com.washer.backend.integration.commerce;

public record CardPaymentResult(String status, String providerOrderNo, String message) {

    public static CardPaymentResult pending(String providerOrderNo, String message) {
        return new CardPaymentResult("pending", providerOrderNo, message);
    }
}
