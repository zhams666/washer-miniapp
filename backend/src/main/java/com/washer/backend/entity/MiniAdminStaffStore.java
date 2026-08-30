package com.washer.backend.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("mini_admin_staff_store")
public class MiniAdminStaffStore {

    @TableId
    private Long id;

    private Long staffId;
    private Long storeId;
    private Integer isPrimary;
    private LocalDateTime createdAt;
}
