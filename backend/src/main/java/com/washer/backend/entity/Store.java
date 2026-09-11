package com.washer.backend.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("store")
public class Store {

    @TableId
    private Long id;

    private Long franchiseeId;
    private String storeCode;
    private String storeName;
    private Integer storeStatus;
    private String province;
    private String city;
    private String district;
    private String address;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private String contactName;
    private String contactPhone;
    private String businessHours;
    private String featureTags;
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
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
