package com.washer.backend.integration.commerce;

public record VoucherVerificationResult(boolean verified, Long storeId, int totalTimes, String sourceChannel, String externalOrderNo, String message) {
}
