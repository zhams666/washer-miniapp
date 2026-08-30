package com.washer.backend.dto.queue;

import lombok.Data;

@Data
public class WashQueueRequest {

    private Long userId;
    private Long storeId;
    private Double userLat;
    private Double userLng;
}
