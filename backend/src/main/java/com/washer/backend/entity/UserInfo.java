package com.washer.backend.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("user_info")
public class UserInfo {

    @TableId
    private Long id;

    private String userNo;
    private String openid;
    private String unionid;
    private String nickname;
    private String realName;
    private String mobile;
    private String avatarUrl;
    private Integer userStatus;
    private String registerSource;
    private Integer isMember;
    private String memberLevel;
    private LocalDateTime memberSinceTime;
    private LocalDateTime lastLoginTime;
    private LocalDateTime lastConsumeTime;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
