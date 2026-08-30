package com.washer.backend.controller;

import com.washer.backend.common.ApiResponse;
import com.washer.backend.entity.PointMallProduct;
import com.washer.backend.entity.PointRedemptionOrder;
import com.washer.backend.service.PointMallProductService;
import com.washer.backend.service.PointRedemptionService;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/point-mall")
public class MiniPointMallController {

    private final PointMallProductService pointMallProductService;
    private final PointRedemptionService pointRedemptionService;

    public MiniPointMallController(PointMallProductService pointMallProductService, PointRedemptionService pointRedemptionService) {
        this.pointMallProductService = pointMallProductService;
        this.pointRedemptionService = pointRedemptionService;
    }

    @GetMapping("/products")
    public ApiResponse<List<Map<String, Object>>> products() {
        return ApiResponse.success(pointMallProductService.listPublishedProducts().stream()
            .map(this::toMiniProduct)
            .toList());
    }

    @PostMapping("/redemptions")
    public ApiResponse<PointRedemptionOrder> redeem(@RequestBody Map<String, Object> payload) {
        return ApiResponse.success(pointRedemptionService.redeem(
            parseLong(payload.get("userId")),
            parseLong(payload.get("productId")),
            payload.get("requestNo") == null ? null : String.valueOf(payload.get("requestNo"))
        ));
    }

    @GetMapping("/redemptions")
    public ApiResponse<List<PointRedemptionOrder>> redemptions(
        @RequestParam Long userId,
        @RequestParam(defaultValue = "20") int limit
    ) {
        return ApiResponse.success(pointRedemptionService.listByUser(userId, limit));
    }

    private Long parseLong(Object value) {
        if (value instanceof Number number) return number.longValue();
        try { return value == null ? null : Long.parseLong(String.valueOf(value)); }
        catch (NumberFormatException ex) { return null; }
    }

    private Map<String, Object> toMiniProduct(PointMallProduct product) {
        return Map.of(
            "id", product.getId(),
            "title", product.getTitle(),
            "description", product.getDescription() == null ? "" : product.getDescription(),
            "coverImage", product.getCoverImage() == null ? "" : product.getCoverImage(),
            "productType", product.getProductType(),
            "pointsPrice", product.getPointsPrice(),
            "limitPerUser", product.getLimitPerUser() == null ? 0 : product.getLimitPerUser()
        );
    }
}
