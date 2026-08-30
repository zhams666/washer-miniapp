package com.washer.backend.dto.admin;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class AdminUserCardAdjustResult {

    private Integer affectedCount = 0;
    private List<Long> userCardIds = new ArrayList<>();
}
