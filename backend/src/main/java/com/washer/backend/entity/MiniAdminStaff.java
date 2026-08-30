package com.washer.backend.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("mini_admin_staff")
public class MiniAdminStaff {

    @TableId
    private Long id;

    private Long franchiseeId;
    private String openid;
    private String staffNo;
    private String staffName;
    private String mobile;
    private String roleCode;
    private String dataScope;
    private Integer status;
    private LocalDateTime lastLoginTime;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
