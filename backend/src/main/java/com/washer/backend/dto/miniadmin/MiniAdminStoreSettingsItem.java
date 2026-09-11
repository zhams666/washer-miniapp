package com.washer.backend.dto.miniadmin;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MiniAdminStoreSettingsItem {

    private Long id;
    private String storeName;
    private String province;
    private String city;
    private String district;
    private String address;
    private String contactName;
    private String contactPhone;
    private String coverImage;
    private Integer doorCloseIntervalOneStart;
    private Integer doorCloseIntervalOneEnd;
    private Integer doorCloseIntervalTwoStart;
    private Integer doorCloseIntervalTwoEnd;
    private BigDecimal registerRewardAmount;
    private BigDecimal inviteRewardAmount;
    private String activityIntro;
    private String rechargeDescription;
    private String memberDescription;
    private BigDecimal cabinetMinRechargeAmount;
    private BigDecimal cabinetMinBalanceAmount;
    private LocalDateTime updatedAt;
}
