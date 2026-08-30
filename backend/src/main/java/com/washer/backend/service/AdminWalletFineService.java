package com.washer.backend.service;

import com.washer.backend.dto.admin.AdminWalletFineRequest;
import com.washer.backend.dto.admin.AdminWalletFineResult;

public interface AdminWalletFineService {

    AdminWalletFineResult manualFine(AdminWalletFineRequest request);
}
