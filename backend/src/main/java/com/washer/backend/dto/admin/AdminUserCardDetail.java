package com.washer.backend.dto.admin;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class AdminUserCardDetail {

    private AdminUserCardPageItem card;
    private List<AdminUserCardUsageItem> usageRecords = new ArrayList<>();
}
