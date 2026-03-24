SET NAMES utf8mb4;

-- =========================
-- 003_schema_enhance.sql
-- 自助洗车系统兼容式增强迁移
-- 说明：
-- 1. 仅做增量增强，不重写、不覆盖既有表结构。
-- 2. 高并发主链路不强制补外键，避免历史脏数据和锁表风险。
-- 3. 优先增强支付快照、计费快照、钱包冻结、退款幂等、结算汇总、归档准备能力。
-- 4. 需要强业务约束但不适合直接落库的内容，统一放到文件末尾“建议由服务层实现”。
-- =========================

-- =========================
-- 1. wash_order：订单级支付状态快照 + 计费快照
-- =========================

ALTER TABLE `wash_order`
  ADD COLUMN `payment_status` VARCHAR(20) NOT NULL DEFAULT 'unpaid' COMMENT '支付状态快照：unpaid/partial/paid/refunding/refunded/failed' AFTER `pay_mode`,
  ADD COLUMN `payment_status_desc` VARCHAR(255) DEFAULT NULL COMMENT '支付状态描述快照' AFTER `payment_status`,
  ADD COLUMN `refund_status_snapshot` VARCHAR(20) NOT NULL DEFAULT 'none' COMMENT '退款状态快照：none/partial/refunding/refunded/failed' AFTER `payment_status_desc`,
  ADD COLUMN `pricing_snapshot` JSON DEFAULT NULL COMMENT '下单时计费规则快照，固化免费时长、首段规则、会员价、优惠限制等' AFTER `refund_status_snapshot`,
  ADD COLUMN `pricing_snapshot_version` INT NOT NULL DEFAULT 1 COMMENT '计费快照版本号' AFTER `pricing_snapshot`;

ALTER TABLE `wash_order`
  ADD KEY `idx_payment_status` (`payment_status`),
  ADD KEY `idx_refund_status_snapshot` (`refund_status_snapshot`);

-- =========================
-- 2. wash_order_payment_detail：混合支付表达能力增强
-- =========================

ALTER TABLE `wash_order_payment_detail`
  ADD COLUMN `payment_seq` INT NOT NULL DEFAULT 1 COMMENT '支付顺序，兼容多段支付/混合支付' AFTER `deduct_times`,
  ADD COLUMN `settle_stage` VARCHAR(20) NOT NULL DEFAULT 'final' COMMENT '结算阶段：prepay/final/refund/adjust' AFTER `payment_seq`,
  ADD COLUMN `allocation_strategy` VARCHAR(30) DEFAULT NULL COMMENT '分摊策略：wallet_first/card_first/manual/rule_engine' AFTER `settle_stage`,
  ADD COLUMN `biz_action_no` VARCHAR(64) DEFAULT NULL COMMENT '业务动作号，用于幂等和补偿追踪' AFTER `allocation_strategy`;

ALTER TABLE `wash_order_payment_detail`
  ADD KEY `idx_order_payment_seq` (`order_id`, `payment_seq`),
  ADD KEY `idx_settle_stage` (`settle_stage`),
  ADD KEY `idx_biz_action_no` (`biz_action_no`);

-- =========================
-- 3. user_daily_discount_record：优惠唯一性增强
--    兼容原有“每用户每天一次”，并支持“每用户每店每天一次”
-- =========================

ALTER TABLE `user_daily_discount_record`
  ADD COLUMN `discount_scope` VARCHAR(30) NOT NULL DEFAULT 'user_day' COMMENT '优惠限制范围：user_day/user_store_day' AFTER `discount_type`,
  ADD COLUMN `scope_store_id` BIGINT NOT NULL DEFAULT 0 COMMENT '作用域门店ID；非按门店限次时固定为0' AFTER `store_id`;

ALTER TABLE `user_daily_discount_record`
  DROP INDEX `uk_user_date_type`,
  ADD UNIQUE KEY `uk_user_discount_scope` (`user_id`, `discount_date`, `discount_type`, `discount_scope`, `scope_store_id`),
  ADD KEY `idx_discount_scope_store` (`discount_scope`, `scope_store_id`);

-- =========================
-- 4. user_store_wallet：冻结能力增强
--    兼容保留原 principal_balance / gift_balance 语义
-- =========================

ALTER TABLE `user_store_wallet`
  ADD COLUMN `available_principal_balance` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '本金可用余额' AFTER `principal_balance`,
  ADD COLUMN `frozen_principal_balance` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '本金冻结余额' AFTER `available_principal_balance`,
  ADD COLUMN `available_gift_balance` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '赠送金可用余额' AFTER `gift_balance`,
  ADD COLUMN `frozen_gift_balance` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '赠送金冻结余额' AFTER `available_gift_balance`;

UPDATE `user_store_wallet`
SET
  `available_principal_balance` = `principal_balance`,
  `available_gift_balance` = `gift_balance`
WHERE `available_principal_balance` = 0.00
  AND `frozen_principal_balance` = 0.00
  AND `available_gift_balance` = 0.00
  AND `frozen_gift_balance` = 0.00;

ALTER TABLE `user_store_wallet`
  ADD KEY `idx_user_store_wallet_available_principal` (`available_principal_balance`),
  ADD KEY `idx_user_store_wallet_available_gift` (`available_gift_balance`);

-- =========================
-- 5. wallet_transaction：支持冻结/释放/预扣类动作
-- =========================

ALTER TABLE `wallet_transaction`
  ADD COLUMN `balance_bucket` VARCHAR(20) NOT NULL DEFAULT 'available' COMMENT '余额桶：available/frozen' AFTER `amount_type`,
  ADD COLUMN `freeze_type` VARCHAR(20) DEFAULT NULL COMMENT '冻结类型：preauth/manual/refund_hold/risk_control' AFTER `change_type`,
  ADD COLUMN `related_action` VARCHAR(30) DEFAULT NULL COMMENT '关联动作：freeze/unfreeze/consume/refund/adjust/expire' AFTER `amount`,
  ADD COLUMN `biz_action_no` VARCHAR(64) DEFAULT NULL COMMENT '业务动作号，用于幂等和补偿追踪' AFTER `related_action`;

ALTER TABLE `wallet_transaction`
  ADD KEY `idx_balance_bucket` (`balance_bucket`),
  ADD KEY `idx_related_action` (`related_action`),
  ADD KEY `idx_wallet_biz_action_no` (`biz_action_no`);

-- =========================
-- 6. payment_transaction：补充幂等支撑字段
-- =========================

ALTER TABLE `payment_transaction`
  ADD COLUMN `request_no` VARCHAR(64) DEFAULT NULL COMMENT '请求号，便于外部调用幂等控制' AFTER `payment_no`,
  ADD COLUMN `biz_action_no` VARCHAR(64) DEFAULT NULL COMMENT '业务动作号，便于补偿任务幂等' AFTER `biz_order_no`,
  ADD COLUMN `idempotency_key` VARCHAR(64) DEFAULT NULL COMMENT '幂等键' AFTER `out_trade_no`;

ALTER TABLE `payment_transaction`
  ADD UNIQUE KEY `uk_payment_request_no` (`request_no`),
  ADD UNIQUE KEY `uk_payment_idempotency_key` (`idempotency_key`),
  ADD KEY `idx_payment_biz_action_no` (`biz_action_no`);

-- =========================
-- 7. payment_callback_log：补充回调幂等和归档准备字段
-- =========================

ALTER TABLE `payment_callback_log`
  ADD COLUMN `callback_no` VARCHAR(64) DEFAULT NULL COMMENT '内部回调流水号' AFTER `payment_no`,
  ADD COLUMN `idempotency_key` VARCHAR(64) DEFAULT NULL COMMENT '回调幂等键' AFTER `channel_trade_no`,
  ADD COLUMN `archive_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '归档标记：0未归档 1已归档' AFTER `raw_content`,
  ADD COLUMN `archive_batch_no` VARCHAR(64) DEFAULT NULL COMMENT '归档批次号' AFTER `archive_flag`,
  ADD COLUMN `archived_at` DATETIME DEFAULT NULL COMMENT '归档时间' AFTER `archive_batch_no`;

ALTER TABLE `payment_callback_log`
  ADD UNIQUE KEY `uk_payment_callback_no` (`callback_no`),
  ADD KEY `idx_payment_callback_idempotency_key` (`idempotency_key`),
  ADD KEY `idx_payment_callback_archive_created` (`archive_flag`, `created_at`);

-- =========================
-- 8. refund_order：退款增强 + 幂等支撑
-- =========================

ALTER TABLE `refund_order`
  ADD COLUMN `refund_channel` VARCHAR(30) DEFAULT NULL COMMENT '退款渠道：wxpay/alipay/wallet/card/manual' AFTER `refund_amount`,
  ADD COLUMN `external_refund_no` VARCHAR(64) DEFAULT NULL COMMENT '外部退款流水号' AFTER `refund_channel`,
  ADD COLUMN `external_refund_status` VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '外部退款状态：pending/success/failed/processing/closed' AFTER `external_refund_no`,
  ADD COLUMN `callback_status` VARCHAR(20) NOT NULL DEFAULT 'none' COMMENT '退款回调状态：none/pending/success/failed/ignored' AFTER `external_refund_status`,
  ADD COLUMN `callback_time` DATETIME DEFAULT NULL COMMENT '退款回调时间' AFTER `refund_time`,
  ADD COLUMN `retry_times` INT NOT NULL DEFAULT 0 COMMENT '退款重试次数' AFTER `callback_time`,
  ADD COLUMN `fail_reason` VARCHAR(255) DEFAULT NULL COMMENT '退款失败原因' AFTER `retry_times`,
  ADD COLUMN `request_no` VARCHAR(64) DEFAULT NULL COMMENT '退款请求号' AFTER `fail_reason`,
  ADD COLUMN `biz_action_no` VARCHAR(64) DEFAULT NULL COMMENT '退款业务动作号' AFTER `request_no`,
  ADD COLUMN `idempotency_key` VARCHAR(64) DEFAULT NULL COMMENT '退款幂等键' AFTER `biz_action_no`;

ALTER TABLE `refund_order`
  ADD UNIQUE KEY `uk_refund_external_refund_no` (`external_refund_no`),
  ADD UNIQUE KEY `uk_refund_request_no` (`request_no`),
  ADD UNIQUE KEY `uk_refund_idempotency_key` (`idempotency_key`),
  ADD KEY `idx_refund_external_status` (`external_refund_status`),
  ADD KEY `idx_refund_callback_status` (`callback_status`),
  ADD KEY `idx_refund_biz_action_no` (`biz_action_no`);

-- =========================
-- 9. device_command_log：设备控制幂等增强
-- =========================

ALTER TABLE `device_command_log`
  ADD COLUMN `request_no` VARCHAR(64) DEFAULT NULL COMMENT '命令请求号' AFTER `command_no`,
  ADD COLUMN `biz_action_no` VARCHAR(64) DEFAULT NULL COMMENT '业务动作号，如订单开始/暂停/停止动作编号' AFTER `order_no`,
  ADD COLUMN `idempotency_key` VARCHAR(64) DEFAULT NULL COMMENT '命令幂等键' AFTER `command_type`;

ALTER TABLE `device_command_log`
  ADD UNIQUE KEY `uk_device_command_request_no` (`request_no`),
  ADD UNIQUE KEY `uk_device_command_idempotency_key` (`idempotency_key`),
  ADD KEY `idx_device_command_biz_action_no` (`biz_action_no`);

-- =========================
-- 10. device：补充工位设备角色建模，保留一对多扩展性
-- =========================

ALTER TABLE `device`
  ADD COLUMN `device_role` VARCHAR(20) NOT NULL DEFAULT 'main' COMMENT '工位内设备角色：main/auxiliary' AFTER `device_type`,
  ADD COLUMN `bay_device_type` VARCHAR(30) DEFAULT NULL COMMENT '工位设备类型：washer/foam/vacuum/controller/other' AFTER `device_role`;

ALTER TABLE `device`
  ADD KEY `idx_bay_role_type` (`bay_id`, `device_role`, `bay_device_type`);

-- =========================
-- 11. store_settlement_bill：新增结算汇总主表
-- =========================

CREATE TABLE IF NOT EXISTS `store_settlement_bill` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `bill_no` VARCHAR(64) NOT NULL COMMENT '结算单号',
  `from_store_id` BIGINT NOT NULL COMMENT '应付门店ID（充值归属门店）',
  `to_store_id` BIGINT NOT NULL COMMENT '应收门店ID（消费发生门店）',
  `settlement_period_type` VARCHAR(20) NOT NULL DEFAULT 'day' COMMENT '结算周期：day/month',
  `start_date` DATE NOT NULL COMMENT '结算起始日期',
  `end_date` DATE NOT NULL COMMENT '结算结束日期',
  `total_order_count` INT NOT NULL DEFAULT 0 COMMENT '订单总数',
  `total_amount` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '结算总金额',
  `total_refund_amount` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '退款总金额',
  `net_amount` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '净结算金额',
  `settlement_status` VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '结算状态：pending/locked/confirmed/paid/cancelled',
  `lock_status` VARCHAR(20) NOT NULL DEFAULT 'unlocked' COMMENT '锁定状态：unlocked/locked',
  `locked_at` DATETIME DEFAULT NULL COMMENT '锁单时间',
  `confirmed_at` DATETIME DEFAULT NULL COMMENT '确认时间',
  `paid_at` DATETIME DEFAULT NULL COMMENT '支付时间',
  `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_store_settlement_bill_no` (`bill_no`),
  UNIQUE KEY `uk_store_settlement_period` (`from_store_id`, `to_store_id`, `settlement_period_type`, `start_date`, `end_date`),
  KEY `idx_bill_from_store` (`from_store_id`),
  KEY `idx_bill_to_store` (`to_store_id`),
  KEY `idx_bill_status` (`settlement_status`),
  KEY `idx_bill_lock_status` (`lock_status`),
  KEY `idx_bill_period` (`settlement_period_type`, `start_date`, `end_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='门店结算汇总主表';

ALTER TABLE `store_settlement_detail`
  ADD COLUMN `bill_id` BIGINT DEFAULT NULL COMMENT '结算汇总主表ID' AFTER `payment_detail_id`,
  ADD COLUMN `bill_no` VARCHAR(64) DEFAULT NULL COMMENT '结算单号快照' AFTER `bill_id`;

ALTER TABLE `store_settlement_detail`
  ADD KEY `idx_bill_id` (`bill_id`),
  ADD KEY `idx_bill_no` (`bill_no`);

-- =========================
-- 12. 高频日志表：归档友好型增强
-- =========================

ALTER TABLE `device_heartbeat_log`
  ADD COLUMN `archive_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '归档标记：0未归档 1已归档' AFTER `payload`,
  ADD COLUMN `archive_batch_no` VARCHAR(64) DEFAULT NULL COMMENT '归档批次号' AFTER `archive_flag`,
  ADD COLUMN `archived_at` DATETIME DEFAULT NULL COMMENT '归档时间' AFTER `archive_batch_no`;

ALTER TABLE `device_heartbeat_log`
  ADD KEY `idx_device_heartbeat_archive_created` (`archive_flag`, `created_at`);

ALTER TABLE `device_event_log`
  ADD COLUMN `archive_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '归档标记：0未归档 1已归档' AFTER `payload`,
  ADD COLUMN `archive_batch_no` VARCHAR(64) DEFAULT NULL COMMENT '归档批次号' AFTER `archive_flag`,
  ADD COLUMN `archived_at` DATETIME DEFAULT NULL COMMENT '归档时间' AFTER `archive_batch_no`;

ALTER TABLE `device_event_log`
  ADD KEY `idx_device_event_archive_created` (`archive_flag`, `created_at`);

ALTER TABLE `operation_audit_log`
  ADD COLUMN `archive_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '归档标记：0未归档 1已归档' AFTER `remark`,
  ADD COLUMN `archive_batch_no` VARCHAR(64) DEFAULT NULL COMMENT '归档批次号' AFTER `archive_flag`,
  ADD COLUMN `archived_at` DATETIME DEFAULT NULL COMMENT '归档时间' AFTER `archive_batch_no`;

ALTER TABLE `operation_audit_log`
  ADD KEY `idx_operation_audit_archive_created` (`archive_flag`, `created_at`);

-- =========================
-- 13. 说明：以下内容暂不直接落库
-- =========================
-- 1. 暂未直接为 store_bay.store_id -> store.id、
--    device.store_id -> store.id、device.bay_id -> store_bay.id
--    增加 FOREIGN KEY。
--    原因：历史库可能存在脏数据；设备/订单链路属于运行中业务，直接补外键存在失败和锁表风险。
--    建议先做数据巡检和离线修复，再结合低峰期评估是否补充外键。
--
-- 2. 暂未对 device 增加“每个工位只能有一个 main 设备”的唯一约束。
--    原因：当前设计需要保留一对多扩展能力，且历史数据未确认是否已存在多个主设备。
--    建议由服务层对 main 角色做写入校验；若后续业务确定，可再补唯一索引或引入主设备映射表。
--
-- 3. 暂未把幂等逻辑本身写入数据库流程。
--    本次仅补 request_no / biz_action_no / idempotency_key 等支撑字段；
--    幂等判重、重试补偿、回调去重建议由服务层实现。
--
-- 4. pricing_snapshot 采用 JSON 字段而非新建快照子表。
--    原因：兼容成本更低，对现有订单链路侵入更小，便于快速固化下单时计费参数。
--    如后续需要对快照做复杂检索和分析，再考虑拆成独立快照表。
--
-- 5. 高频日志表本次仅增加 archive_flag/archive_batch_no/archived_at 与归档索引。
--    后续建议按月分表或冷热分层，不在本次迁移中直接执行分表改造。
