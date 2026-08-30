package com.washer.backend.dto.miniadmin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MiniAdminStoreOption {

    private Long id;
    private Long franchiseeId;
    private String storeName;
}
