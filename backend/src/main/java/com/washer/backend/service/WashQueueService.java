package com.washer.backend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.washer.backend.dto.queue.WashQueueRequest;
import com.washer.backend.entity.WashQueue;
import java.util.Map;

public interface WashQueueService extends IService<WashQueue> {

    Map<String, Object> joinQueue(WashQueueRequest request);

    Map<String, Object> checkQueueLocation(WashQueueRequest request);

    Map<String, Object> getQueueStatus(Long userId, Long storeId);
}
