package com.washer.backend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.washer.backend.dto.admin.AdminStoreOption;
import com.washer.backend.entity.Store;
import com.washer.backend.mapper.StoreMapper;
import com.washer.backend.service.StoreService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class StoreServiceImpl extends ServiceImpl<StoreMapper, Store> implements StoreService {

    @Override
    public List<AdminStoreOption> getAdminStoreOptions() {
        return this.lambdaQuery()
            .orderByAsc(Store::getId)
            .list()
            .stream()
            .map(store -> new AdminStoreOption(store.getId(), store.getStoreName()))
            .toList();
    }
}
