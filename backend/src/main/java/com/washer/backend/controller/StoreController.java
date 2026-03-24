package com.washer.backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.washer.backend.common.ApiResponse;
import com.washer.backend.entity.Store;
import com.washer.backend.service.StoreService;
import java.util.UUID;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stores")
public class StoreController {

    private final StoreService storeService;

    public StoreController(StoreService storeService) {
        this.storeService = storeService;
    }

    @GetMapping
    public ApiResponse<Page<Store>> page(
        @RequestParam(defaultValue = "1") long page,
        @RequestParam(defaultValue = "10") long size,
        @RequestParam(required = false) String keyword
    ) {
        LambdaQueryWrapper<Store> wrapper = new LambdaQueryWrapper<Store>()
            .orderByDesc(Store::getId);

        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w
                .like(Store::getStoreName, keyword)
                .or()
                .like(Store::getStoreCode, keyword)
                .or()
                .like(Store::getAddress, keyword));
        }

        return ApiResponse.success(storeService.page(new Page<>(page, size), wrapper));
    }

    @GetMapping("/{id}")
    public ApiResponse<Store> getById(@PathVariable Long id) {
        Store store = storeService.getById(id);
        if (store == null) {
            throw new IllegalArgumentException("门店不存在");
        }
        return ApiResponse.success(store);
    }

    @PostMapping
    public ApiResponse<Store> create(@RequestBody Store store) {
        if (!StringUtils.hasText(store.getStoreCode())) {
            store.setStoreCode("S" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
        }
        if (!StringUtils.hasText(store.getStoreName())) {
            throw new IllegalArgumentException("storeName 不能为空");
        }
        if (store.getStoreStatus() == null) {
            store.setStoreStatus(1);
        }
        storeService.save(store);
        return ApiResponse.success("创建成功", store);
    }

    @PutMapping("/{id}")
    public ApiResponse<Store> update(@PathVariable Long id, @RequestBody Store store) {
        store.setId(id);
        boolean updated = storeService.updateById(store);
        if (!updated) {
            throw new IllegalArgumentException("门店不存在或更新失败");
        }
        return ApiResponse.success("更新成功", storeService.getById(id));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Boolean> delete(@PathVariable Long id) {
        boolean removed = storeService.removeById(id);
        if (!removed) {
            throw new IllegalArgumentException("门店不存在或删除失败");
        }
        return ApiResponse.success("删除成功", true);
    }
}
