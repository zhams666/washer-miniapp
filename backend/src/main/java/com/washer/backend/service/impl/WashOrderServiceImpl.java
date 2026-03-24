package com.washer.backend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.washer.backend.entity.WashOrder;
import com.washer.backend.mapper.WashOrderMapper;
import com.washer.backend.service.WashOrderService;
import org.springframework.stereotype.Service;

@Service
public class WashOrderServiceImpl extends ServiceImpl<WashOrderMapper, WashOrder> implements WashOrderService {
}
