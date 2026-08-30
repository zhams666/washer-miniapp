SET NAMES utf8mb4;

ALTER TABLE `user_info`
  ADD COLUMN `member_expire_time` DATETIME DEFAULT NULL COMMENT '会员到期时间，空表示历史永久会员' AFTER `member_since_time`;

CREATE TABLE IF NOT EXISTS `membership_setting` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `setting_key` VARCHAR(40) NOT NULL,
  `member_day_enabled` TINYINT NOT NULL DEFAULT 1,
  `member_day_weekday` TINYINT NOT NULL DEFAULT 3,
  `member_day_start_time` TIME NOT NULL DEFAULT '00:00:00',
  `member_day_end_time` TIME NOT NULL DEFAULT '23:59:59',
  `member_day_first_minutes` INT NOT NULL DEFAULT 10,
  `member_day_discount_rate` DECIMAL(5,4) NOT NULL DEFAULT 0.7500,
  `benefit_text` VARCHAR(255) DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_membership_setting_key` (`setting_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `membership_plan` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `plan_code` VARCHAR(40) NOT NULL,
  `plan_name` VARCHAR(100) NOT NULL,
  `plan_type` VARCHAR(20) NOT NULL,
  `duration_months` INT NOT NULL,
  `price` DECIMAL(18,2) NOT NULL DEFAULT 0.00,
  `benefit_text` VARCHAR(255) DEFAULT NULL,
  `status` TINYINT NOT NULL DEFAULT 1,
  `sort_order` INT NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_membership_plan_code` (`plan_code`),
  KEY `idx_membership_plan_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `membership_order` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `order_no` VARCHAR(64) NOT NULL,
  `user_id` BIGINT NOT NULL,
  `plan_id` BIGINT NOT NULL,
  `pay_amount` DECIMAL(18,2) NOT NULL DEFAULT 0.00,
  `pay_channel` VARCHAR(30) DEFAULT NULL,
  `pay_status` VARCHAR(20) NOT NULL DEFAULT 'pending',
  `payment_no` VARCHAR(64) DEFAULT NULL,
  `third_party_trade_no` VARCHAR(64) DEFAULT NULL,
  `pay_time` DATETIME DEFAULT NULL,
  `member_start_time` DATETIME DEFAULT NULL,
  `member_expire_time` DATETIME DEFAULT NULL,
  `remark` VARCHAR(255) DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_membership_order_no` (`order_no`),
  KEY `idx_membership_order_user` (`user_id`),
  KEY `idx_membership_order_status` (`pay_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
