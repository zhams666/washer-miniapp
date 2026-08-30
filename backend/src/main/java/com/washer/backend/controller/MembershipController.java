package com.washer.backend.controller;

import com.washer.backend.common.ApiResponse;
import com.washer.backend.service.MembershipService;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/membership")
public class MembershipController {

    private final MembershipService membershipService;

    public MembershipController(MembershipService membershipService) {
        this.membershipService = membershipService;
    }

    @GetMapping("/overview")
    public ApiResponse<Map<String, Object>> overview(@RequestParam(required = false) Long userId) {
        return ApiResponse.success(membershipService.getOverview(userId));
    }

    @PostMapping("/orders")
    public ApiResponse<Map<String, Object>> createOrder(@RequestBody Map<String, Object> payload) {
        Long userId = parseLong(payload, "userId");
        Long planId = parseLong(payload, "planId", "membershipPlanId");
        String openId = text(payload, "openId", "openid");
        return ApiResponse.success(membershipService.createOrder(userId, planId, openId));
    }

    @GetMapping("/orders/{orderNo}")
    public ApiResponse<Map<String, Object>> order(@PathVariable String orderNo) {
        return ApiResponse.success(membershipService.getOrderResult(orderNo));
    }

    @PostMapping("/orders/{orderNo}/sync")
    public ApiResponse<Map<String, Object>> sync(@PathVariable String orderNo) {
        return ApiResponse.success(membershipService.syncOrder(orderNo));
    }

    private Long parseLong(Map<String, Object> payload, String... keys) {
        for (String key : keys) {
            Object value = payload == null ? null : payload.get(key);
            if (value != null) {
                try {
                    return Long.valueOf(String.valueOf(value));
                } catch (NumberFormatException ignored) {
                    return null;
                }
            }
        }
        return null;
    }

    private String text(Map<String, Object> payload, String... keys) {
        if (payload == null) {
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
