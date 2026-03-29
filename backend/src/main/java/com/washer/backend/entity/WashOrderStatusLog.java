package com.washer.backend.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("wash_order_status_log")
public class WashOrderStatusLog {

    @TableId
    private Long id;

    private Long orderId;
    private String orderNo;
    private String fromStatus;
    private String toStatus;
    private String actionType;
    private String operatorType;
    private Long operatorId;
    private String remark;
    private LocalDateTime createdAt;
}
