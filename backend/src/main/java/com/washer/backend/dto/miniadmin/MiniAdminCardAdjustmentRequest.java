package com.washer.backend.dto.miniadmin;

import lombok.Data;

@Data
public class MiniAdminCardAdjustmentRequest {

    private Long userId;
    private Long storeId;
    private Long userCardId;
    private Integer deltaTimes;
    private String remark;
}
