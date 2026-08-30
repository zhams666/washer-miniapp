package com.washer.backend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.washer.backend.common.ApiResponse;
import com.washer.backend.entity.PointMallProduct;
import com.washer.backend.service.PointMallProductService;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/point-mall/products")
public class AdminPointMallProductController {

    private final PointMallProductService pointMallProductService;

    public AdminPointMallProductController(PointMallProductService pointMallProductService) {
        this.pointMallProductService = pointMallProductService;
    }

    @GetMapping
    public ApiResponse<Page<PointMallProduct>> page(
        @RequestParam(defaultValue = "1") long page,
        @RequestParam(defaultValue = "10") long size,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) Integer status,
        @RequestParam(required = false) String productType
    ) {
        return ApiResponse.success(pointMallProductService.pageProducts(page, size, keyword, status, productType));
    }

    @PostMapping
    public ApiResponse<PointMallProduct> create(@RequestBody Map<String, Object> payload) {
        return ApiResponse.success("积分商品已创建", pointMallProductService.saveProduct(null, payload));
    }

    @PutMapping("/{id}")
    public ApiResponse<PointMallProduct> update(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        return ApiResponse.success("积分商品已更新", pointMallProductService.saveProduct(id, payload));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<PointMallProduct> changeStatus(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        return ApiResponse.success("积分商品状态已更新", pointMallProductService.changeStatus(id, toInteger(payload.get("status"))));
    }

    private Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return Integer.valueOf(String.valueOf(value));
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("商品状态不合法");
        }
    }
}
