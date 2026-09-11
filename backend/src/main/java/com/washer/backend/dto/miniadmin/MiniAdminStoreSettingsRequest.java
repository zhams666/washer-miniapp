package com.washer.backend.dto.miniadmin;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class MiniAdminStoreSettingsRequest {

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
}
