package com.washer.backend.controller;

import com.washer.backend.common.ApiResponse;
import com.washer.backend.service.StoreExchangeVoucherService;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cards/exchange-vouchers")
public class StoreExchangeVoucherController {

    private final StoreExchangeVoucherService storeExchangeVoucherService;

    public StoreExchangeVoucherController(StoreExchangeVoucherService storeExchangeVoucherService) {
        this.storeExchangeVoucherService = storeExchangeVoucherService;
    }

    @PostMapping("/redeem")
    public ApiResponse<Map<String, Object>> redeem(@RequestBody Map<String, Object> payload) {
        Long userId = parseLong(payload != null ? payload.get("userId") : null);
        Long storeId = parseLong(payload != null ? payload.get("storeId") : null);
        String serialNo = getString(payload, "serialNo", "voucherCode", "code");
        return ApiResponse.success(storeExchangeVoucherService.redeem(userId, storeId, serialNo));
    }

    private Long parseLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            String text = String.valueOf(value).trim();
            return text.isEmpty() ? null : Long.parseLong(text);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String getString(Map<String, Object> payload, String... keys) {
        if (payload == null || keys == null) {
            return null;
        }
        for (String key : keys) {
            Object value = payload.get(key);
            if (value != null && !String.valueOf(value).trim().isEmpty()) {
                return String.valueOf(value).trim();
            }
        }
        return null;
    }
}
