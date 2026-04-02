package com.washer.backend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.washer.backend.dto.admin.AdminStoreOption;
import com.washer.backend.entity.Store;
import java.util.List;

public interface StoreService extends IService<Store> {

    List<AdminStoreOption> getAdminStoreOptions();
}
