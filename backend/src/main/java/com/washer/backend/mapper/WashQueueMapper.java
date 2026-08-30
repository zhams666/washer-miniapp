package com.washer.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.washer.backend.entity.WashQueue;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface WashQueueMapper extends BaseMapper<WashQueue> {
}
