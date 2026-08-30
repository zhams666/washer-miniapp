package com.washer.backend.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("franchisee")
public class Franchisee {

    @TableId
    private Long id;

    private String franchiseeCode;
    private String franchiseeName;
    private String contactName;
    private String contactPhone;
    private Integer status;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
