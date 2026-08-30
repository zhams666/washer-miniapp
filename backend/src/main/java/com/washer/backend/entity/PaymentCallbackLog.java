package com.washer.backend.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("payment_callback_log")
public class PaymentCallbackLog {

    @TableId
    private Long id;

    private Long paymentTransactionId;
    private String paymentNo;
    private String callbackNo;
    private String bizOrderNo;
    private String payChannel;
    private String callbackType;
    private String channelTradeNo;
    private String idempotencyKey;
    private LocalDateTime notifyTime;
    private Integer signVerified;
    private String processStatus;
    private String processResult;
    private String rawContent;
    private Integer archiveFlag;
    private String archiveBatchNo;
    private LocalDateTime archivedAt;
    private LocalDateTime createdAt;
}
