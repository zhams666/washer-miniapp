package com.washer.backend.controller;

import com.washer.backend.common.ApiResponse;
import com.washer.backend.dto.admin.AdminWalletFineRequest;
import com.washer.backend.dto.admin.AdminWalletFineResult;
import com.washer.backend.service.AdminWalletFineService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminWalletFineController {

    private final AdminWalletFineService adminWalletFineService;

    public AdminWalletFineController(AdminWalletFineService adminWalletFineService) {
        this.adminWalletFineService = adminWalletFineService;
    }

    @PostMapping("/wallet-fines")
    public ApiResponse<AdminWalletFineResult> manualFine(@RequestBody AdminWalletFineRequest request) {
        return ApiResponse.success(adminWalletFineService.manualFine(request));
    }
}
