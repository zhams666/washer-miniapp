package com.washer.backend.dto.admin;

import java.util.List;
import lombok.Data;

@Data
public class AdminUserCardManualReduceRequest {

    private Long storeId;
    private Integer count;
    private List<Long> userCardIds;
    private String remark;
}
