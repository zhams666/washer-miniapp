package com.washer.backend.controller;

import com.washer.backend.common.ApiResponse;
import com.washer.backend.dto.queue.WashQueueRequest;
import com.washer.backend.service.WashQueueService;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/queues")
public class WashQueueController {

    private final WashQueueService washQueueService;

    public WashQueueController(WashQueueService washQueueService) {
        this.washQueueService = washQueueService;
    }

    @PostMapping("/join")
    public ApiResponse<Map<String, Object>> join(@RequestBody WashQueueRequest request) {
        return ApiResponse.success("queued", washQueueService.joinQueue(request));
    }

    @PostMapping("/check-location")
    public ApiResponse<Map<String, Object>> checkLocation(@RequestBody WashQueueRequest request) {
        return ApiResponse.success(washQueueService.checkQueueLocation(request));
    }

    @GetMapping("/status")
    public ApiResponse<Map<String, Object>> status(
        @RequestParam Long userId,
        @RequestParam Long storeId
    ) {
        return ApiResponse.success(washQueueService.getQueueStatus(userId, storeId));
    }
}
