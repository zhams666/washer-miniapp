package com.washer.backend.dto.admin;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AdminUserListItem {

    private Long id;
    private String userNo;
    private String nickname;
    private String realName;
    private String mobile;
    private Integer userStatus;
    private String registerSource;
    private Integer isMember;
    private String memberLevel;
    private Integer points;
    private LocalDateTime memberExpireTime;
    private LocalDateTime lastConsumeTime;
    private LocalDateTime createdAt;
}
