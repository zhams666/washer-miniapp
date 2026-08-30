package com.washer.backend.dto.miniadmin;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MiniAdminUserSearchItem {

    private Long id;
    private String userNo;
    private String nickname;
    private String realName;
    private String mobile;
    private String avatarUrl;
    private Integer userStatus;
    private BigDecimal principalBalance;
    private BigDecimal giftBalance;
    private Integer remainingCardTimes;
}
