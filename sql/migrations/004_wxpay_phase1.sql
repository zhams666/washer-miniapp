-- 微信支付第一阶段：补充预支付、失败原因和钱包入账幂等约束。

ALTER TABLE `payment_transaction`
  ADD COLUMN `prepay_id` VARCHAR(128) DEFAULT NULL COMMENT '微信预支付交易会话标识' AFTER `out_trade_no`,
  ADD COLUMN `fail_reason` VARCHAR(255) DEFAULT NULL COMMENT '支付失败或关闭原因' AFTER `remark`;

ALTER TABLE `wallet_transaction`
  ADD UNIQUE KEY `uk_wallet_recharge_action_amount_type` (`biz_action_no`, `amount_type`);
