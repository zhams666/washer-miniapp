SET NAMES utf8mb4;

-- =========================
-- 002_schema_upgrade.sql
-- 自助洗车系统生产增强迁移
-- 说明：
-- 1. 本迁移基于现有初始化库做兼容式增强，不删除现有表
-- 2. 高频交易链路暂不强制增加外键，仍由服务层保证业务一致性
-- 3. 迁移内容包括：索引优化、金额字段扩容、支付退款增强、设备事件补齐、审计增强
-- =========================

-- =========================
-- 1. wash_order 索引增强
-- =========================

ALTER TABLE `wash_order`
  ADD KEY `idx_device_status` (`device_id`, `order_status`),
  ADD KEY `idx_bay_status` (`bay_id`, `order_status`),
  ADD KEY `idx_user_status` (`user_id`, `order_status`);

-- =========================
-- 2. 高频查询索引增强
-- =========================

ALTER TABLE `wallet_transaction`
  ADD KEY `idx_user_created_at` (`user_id`, `created_at`);

ALTER TABLE `device_heartbeat_log`
  ADD KEY `idx_device_created_at` (`device_id`, `created_at`);

-- 说明：
-- device_command_log 当前没有 created_at 字段，保留 request_time 作为命令时间主索引。
ALTER TABLE `device_command_log`
  ADD KEY `idx_device_request_time` (`device_id`, `request_time`);

-- =========================
-- 3. 金额字段扩容：DECIMAL(10,2) -> DECIMAL(18,2)
-- =========================

ALTER TABLE `user_store_wallet`
  MODIFY COLUMN `principal_balance` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '本金余额（可跨店使用，可退款）',
  MODIFY COLUMN `gift_balance` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '赠送余额（仅本店使用，不可退款）',
  MODIFY COLUMN `total_recharge_principal` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '累计充值本金',
  MODIFY COLUMN `total_recharge_gift` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '累计充值赠送金额',
  MODIFY COLUMN `total_consume_principal` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '累计消费本金',
  MODIFY COLUMN `total_consume_gift` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '累计消费赠送金额',
  MODIFY COLUMN `total_refund_principal` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '累计退款本金';

ALTER TABLE `wallet_transaction`
  MODIFY COLUMN `amount` DECIMAL(18,2) NOT NULL COMMENT '变动金额',
  MODIFY COLUMN `balance_before` DECIMAL(18,2) NOT NULL COMMENT '变动前余额',
  MODIFY COLUMN `balance_after` DECIMAL(18,2) NOT NULL COMMENT '变动后余额';

ALTER TABLE `recharge_order`
  MODIFY COLUMN `principal_amount` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '本金金额',
  MODIFY COLUMN `gift_amount` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '赠送金额',
  MODIFY COLUMN `pay_amount` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '实际支付金额';

ALTER TABLE `wash_order`
  MODIFY COLUMN `estimated_amount` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '预估金额',
  MODIFY COLUMN `final_amount` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '最终金额',
  MODIFY COLUMN `paid_amount` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '已支付金额',
  MODIFY COLUMN `refund_amount` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '已退款金额',
  MODIFY COLUMN `first_period_discount_amount` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '首段优惠金额';

ALTER TABLE `wash_order_payment_detail`
  MODIFY COLUMN `amount` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '支付金额',
  MODIFY COLUMN `refunded_amount` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '已退款金额';

ALTER TABLE `refund_order`
  MODIFY COLUMN `refund_amount` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '退款金额';

ALTER TABLE `store_settlement_detail`
  MODIFY COLUMN `principal_amount` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '本金消费金额',
  MODIFY COLUMN `refund_adjust_amount` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '退款冲减金额',
  MODIFY COLUMN `net_amount` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '实际结算金额';

-- =========================
-- 4. 支付增强：支付交易主表
-- =========================

CREATE TABLE IF NOT EXISTS `payment_transaction` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `payment_no` VARCHAR(64) NOT NULL COMMENT '支付流水号',
  `biz_type` VARCHAR(30) NOT NULL COMMENT '业务类型：recharge/card_purchase/other',
  `biz_order_id` BIGINT DEFAULT NULL COMMENT '业务订单ID',
  `biz_order_no` VARCHAR(64) NOT NULL COMMENT '业务订单号',
  `user_id` BIGINT DEFAULT NULL COMMENT '用户ID',
  `store_id` BIGINT DEFAULT NULL COMMENT '门店ID',
  `pay_channel` VARCHAR(30) NOT NULL COMMENT '支付渠道：wxpay/alipay/cash/manual',
  `channel_trade_no` VARCHAR(64) DEFAULT NULL COMMENT '渠道交易号',
  `out_trade_no` VARCHAR(64) DEFAULT NULL COMMENT '商户侧请求单号',
  `pay_status` VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '支付状态：pending/paid/failed/closed/refunded',
  `pay_amount` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '支付金额',
  `currency_code` VARCHAR(10) NOT NULL DEFAULT 'CNY' COMMENT '币种',
  `callback_count` INT NOT NULL DEFAULT 0 COMMENT '回调次数',
  `request_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '支付发起时间',
  `pay_time` DATETIME DEFAULT NULL COMMENT '支付成功时间',
  `callback_time` DATETIME DEFAULT NULL COMMENT '最后一次回调时间',
  `expire_time` DATETIME DEFAULT NULL COMMENT '支付过期时间',
  `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_no` (`payment_no`),
  UNIQUE KEY `uk_out_trade_no` (`out_trade_no`),
  KEY `idx_biz_order_no` (`biz_order_no`),
  KEY `idx_biz_type_status` (`biz_type`, `pay_status`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_store_id` (`store_id`),
  KEY `idx_pay_channel_status` (`pay_channel`, `pay_status`),
  KEY `idx_request_time` (`request_time`),
  KEY `idx_callback_time` (`callback_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付交易表';

CREATE TABLE IF NOT EXISTS `payment_callback_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `payment_transaction_id` BIGINT DEFAULT NULL COMMENT '支付交易ID',
  `payment_no` VARCHAR(64) DEFAULT NULL COMMENT '支付流水号',
  `biz_order_no` VARCHAR(64) DEFAULT NULL COMMENT '业务订单号',
  `pay_channel` VARCHAR(30) NOT NULL COMMENT '支付渠道',
  `callback_type` VARCHAR(30) NOT NULL DEFAULT 'payment_notify' COMMENT '回调类型：payment_notify/refund_notify/other',
  `channel_trade_no` VARCHAR(64) DEFAULT NULL COMMENT '渠道交易号',
  `notify_time` DATETIME DEFAULT NULL COMMENT '渠道通知时间',
  `sign_verified` TINYINT NOT NULL DEFAULT 0 COMMENT '是否验签通过：1是 0否',
  `process_status` VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '处理状态：pending/success/failed/ignored',
  `process_result` VARCHAR(255) DEFAULT NULL COMMENT '处理结果摘要',
  `raw_content` LONGTEXT DEFAULT NULL COMMENT '原始回调内容',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_payment_transaction_id` (`payment_transaction_id`),
  KEY `idx_payment_no` (`payment_no`),
  KEY `idx_biz_order_no` (`biz_order_no`),
  KEY `idx_pay_channel` (`pay_channel`),
  KEY `idx_notify_time` (`notify_time`),
  KEY `idx_process_status` (`process_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付回调日志表';

CREATE TABLE IF NOT EXISTS `refund_payment_detail` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `refund_order_id` BIGINT NOT NULL COMMENT '退款单ID',
  `refund_order_no` VARCHAR(64) DEFAULT NULL COMMENT '退款单号',
  `order_id` BIGINT DEFAULT NULL COMMENT '原洗车订单ID',
  `order_no` VARCHAR(64) DEFAULT NULL COMMENT '原洗车订单号',
  `order_payment_detail_id` BIGINT NOT NULL COMMENT '原订单支付明细ID',
  `source_type` VARCHAR(20) NOT NULL COMMENT '支付来源类型：wallet/card',
  `amount_type` VARCHAR(20) DEFAULT NULL COMMENT '金额类型：principal/gift/card',
  `refund_amount` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '退款金额',
  `refund_times` INT NOT NULL DEFAULT 0 COMMENT '退回次数，次卡退款时使用',
  `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_refund_order_id` (`refund_order_id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_order_payment_detail_id` (`order_payment_detail_id`),
  KEY `idx_source_type` (`source_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='退款支付明细表';

-- =========================
-- 5. 设备事件增强
-- =========================

CREATE TABLE IF NOT EXISTS `device_event_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `device_id` BIGINT NOT NULL COMMENT '设备ID',
  `device_code` VARCHAR(64) NOT NULL COMMENT '设备编码',
  `event_type` VARCHAR(30) NOT NULL COMMENT '事件类型：status/fault/action/sensor/other',
  `event_code` VARCHAR(64) NOT NULL COMMENT '事件编码',
  `event_time` DATETIME NOT NULL COMMENT '事件发生时间',
  `order_id` BIGINT DEFAULT NULL COMMENT '关联订单ID',
  `order_no` VARCHAR(64) DEFAULT NULL COMMENT '关联订单号',
  `severity` VARCHAR(20) NOT NULL DEFAULT 'info' COMMENT '事件级别：info/warn/error/critical',
  `payload` LONGTEXT DEFAULT NULL COMMENT '事件原始内容',
  `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_device_id` (`device_id`),
  KEY `idx_device_code` (`device_code`),
  KEY `idx_event_time` (`event_time`),
  KEY `idx_event_type` (`event_type`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_severity` (`severity`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备事件日志表';

-- =========================
-- 6. 审计能力增强
-- =========================

CREATE TABLE IF NOT EXISTS `operation_audit_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `operator_type` VARCHAR(20) NOT NULL COMMENT '操作人类型：system/admin/user/device',
  `operator_id` BIGINT DEFAULT NULL COMMENT '操作人ID',
  `target_type` VARCHAR(30) NOT NULL COMMENT '目标类型：order/wallet/refund/device/card/user/store/other',
  `target_id` BIGINT NOT NULL COMMENT '目标ID',
  `action_type` VARCHAR(30) NOT NULL COMMENT '动作类型：create/update/delete/approve/reject/refund/adjust/other',
  `change_summary` VARCHAR(500) DEFAULT NULL COMMENT '变更摘要',
  `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_operator` (`operator_type`, `operator_id`),
  KEY `idx_target` (`target_type`, `target_id`),
  KEY `idx_action_type` (`action_type`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作审计日志表';
