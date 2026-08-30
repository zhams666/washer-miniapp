-- 钱包充值商品化：真实支付前禁止信任前端金额和赠送金额

CREATE TABLE IF NOT EXISTS `wallet_recharge_product` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `store_id` BIGINT NOT NULL COMMENT '所属门店ID',
  `product_name` VARCHAR(100) NOT NULL COMMENT '充值商品名称',
  `pay_amount` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '实际支付金额',
  `principal_amount` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '到账本金金额',
  `gift_amount` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '到账赠送金额',
  `effective_time` DATETIME DEFAULT NULL COMMENT '生效时间，空表示立即生效',
  `expire_time` DATETIME DEFAULT NULL COMMENT '失效时间，空表示长期有效',
  `purchase_limit` INT NOT NULL DEFAULT 0 COMMENT '每用户购买次数限制，0表示不限制',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1上架 0下架',
  `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_store_id` (`store_id`),
  KEY `idx_status` (`status`),
  KEY `idx_effective_expire` (`effective_time`, `expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='钱包充值商品表';

ALTER TABLE `recharge_order`
  ADD COLUMN `recharge_product_id` BIGINT DEFAULT NULL COMMENT '充值商品ID' AFTER `store_id`,
  ADD KEY `idx_recharge_product_id` (`recharge_product_id`);
