package com.washer.backend.dto.miniadmin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MiniAdminScopeSummaryItem {

    private String key;
    private String title;
    private Long count;
    private String unit;
    private String description;
}
