package com.washer.backend.dto.miniadmin;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MiniAdminUserCardItem {

    private Long id;
    private Long storeId;
    private String storeName;
    private String cardNo;
    private String cardType;
    private Integer totalTimes;
    private Integer usedTimes;
    private Integer remainingTimes;
    private String status;
    private LocalDateTime expireTime;
    private String remark;
}
