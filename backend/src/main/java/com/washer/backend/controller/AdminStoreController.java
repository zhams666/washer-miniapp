package com.washer.backend.controller;

import com.washer.backend.common.ApiResponse;
import com.washer.backend.dto.admin.AdminStoreOption;
import com.washer.backend.service.StoreService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/stores")
public class AdminStoreController {

    private final StoreService storeService;

    public AdminStoreController(StoreService storeService) {
        this.storeService = storeService;
    }

    @GetMapping("/options")
    public ApiResponse<List<AdminStoreOption>> options() {
        return ApiResponse.success(storeService.getAdminStoreOptions());
    }
}
