package com.washer.backend.integration.commerce;

/** Fill this adapter with the Meituan, Douyin, or other voucher verification API. */
public interface VoucherVerificationGateway {

    VoucherVerificationResult verify(String voucherCode, Long storeId, String sourceChannel);
}
