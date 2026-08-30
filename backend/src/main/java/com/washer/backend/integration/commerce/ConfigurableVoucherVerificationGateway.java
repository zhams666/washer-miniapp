package com.washer.backend.integration.commerce;

import com.washer.backend.config.CommerceProperties;
import org.springframework.stereotype.Component;

/** Default provider-mode adapter. Replace it with a signed upstream voucher verification call. */
@Component
public class ConfigurableVoucherVerificationGateway implements VoucherVerificationGateway {

    private final CommerceProperties properties;

    public ConfigurableVoucherVerificationGateway(CommerceProperties properties) {
        this.properties = properties;
    }

    @Override
    public VoucherVerificationResult verify(String voucherCode, Long storeId, String sourceChannel) {
        return new VoucherVerificationResult(false, storeId, 0, sourceChannel, null,
            "voucher provider " + properties.getProviderName() + " is not configured");
    }
}
