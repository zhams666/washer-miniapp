SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `mini_admin_asset_operation` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `operation_no` VARCHAR(64) NOT NULL COMMENT 'Operation number',
  `operation_type` VARCHAR(40) NOT NULL COMMENT 'wallet_adjust/fine/card_adjust',
  `change_type` VARCHAR(20) DEFAULT NULL COMMENT 'in/out',
  `user_id` BIGINT NOT NULL COMMENT 'User ID',
  `store_id` BIGINT NOT NULL COMMENT 'Store ID',
  `wallet_id` BIGINT DEFAULT NULL COMMENT 'Wallet ID',
  `user_card_id` BIGINT DEFAULT NULL COMMENT 'User card ID',
  `amount_type` VARCHAR(20) DEFAULT NULL COMMENT 'principal/gift/mixed',
  `principal_amount` DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT 'Principal amount',
  `gift_amount` DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT 'Gift amount',
  `total_amount` DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT 'Total amount',
  `card_delta_times` INT NOT NULL DEFAULT 0 COMMENT 'Card times delta',
  `operator_staff_id` BIGINT NOT NULL COMMENT 'Mini admin staff ID',
  `operator_role_code` VARCHAR(40) DEFAULT NULL COMMENT 'Operator role',
  `remark` VARCHAR(255) NOT NULL COMMENT 'Operation remark',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_asset_operation_no` (`operation_no`),
  KEY `idx_asset_operation_user` (`user_id`, `created_at`),
  KEY `idx_asset_operation_store` (`store_id`, `created_at`),
  KEY `idx_asset_operation_type` (`operation_type`, `created_at`),
  KEY `idx_asset_operation_operator` (`operator_staff_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Mini admin asset operation audit';

INSERT INTO `mini_admin_staff` (
  `id`,
  `franchisee_id`,
  `openid`,
  `staff_no`,
  `staff_name`,
  `mobile`,
  `role_code`,
  `data_scope`,
  `status`,
  `remark`
) VALUES (
  104,
  1,
  'mock_openid_staff',
  'STAFF_STORE_1_DEMO',
  '门店一员工演示号',
  '',
  'store_staff',
  'store',
  1,
  'local demo store staff'
) AS new
ON DUPLICATE KEY UPDATE
  `franchisee_id` = new.`franchisee_id`,
  `openid` = new.`openid`,
  `staff_name` = new.`staff_name`,
  `role_code` = new.`role_code`,
  `data_scope` = new.`data_scope`,
  `status` = new.`status`,
  `remark` = new.`remark`;

INSERT INTO `mini_admin_staff_store` (
  `staff_id`,
  `store_id`,
  `is_primary`,
  `created_at`
) VALUES (
  104,
  1,
  1,
  NOW()
) AS new
ON DUPLICATE KEY UPDATE
  `is_primary` = new.`is_primary`;
