package com.washer.backend.dto.admin;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AdminUserAssetOverview {

    private Long id;
    private String userNo;
    private String nickname;
    private String realName;
    private String mobile;
    private Integer userStatus;
    private String registerSource;
    private Integer isMember;
    private String memberLevel;
    private LocalDateTime memberSinceTime;
    private LocalDateTime lastLoginTime;
    private LocalDateTime lastConsumeTime;
    private String remark;
    private LocalDateTime createdAt;
    private Integer todayFirstPeriodDiscountUsed;
    private LocalDate todayFirstPeriodDiscountDate;
    private Long todayFirstPeriodDiscountStoreId;
    private String todayFirstPeriodDiscountStoreName;
    private Long todayFirstPeriodDiscountOrderId;
    private String todayFirstPeriodDiscountOrderNo;
    private BigDecimal todayFirstPeriodDiscountAmount;
    private List<AdminUserWalletAsset> wallets;
    private List<AdminUserCardAsset> cards;
    private List<AdminUserRecentOrder> recentOrders;
    private List<AdminUserWalletTransactionItem> recentWalletTransactions;
    private List<AdminUserCardUsageItem> recentCardUsages;
}
