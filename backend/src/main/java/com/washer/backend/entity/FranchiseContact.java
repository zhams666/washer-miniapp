package com.washer.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("franchise_contact")
public class FranchiseContact {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String contactName;
    private String contactPhone;
    private String source;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
