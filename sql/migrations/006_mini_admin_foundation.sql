SET NAMES utf8mb4;

-- Mini admin / franchise foundation.
-- Keep this layer independent from customer miniapp identity.

CREATE TABLE IF NOT EXISTS `franchisee` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `franchisee_code` VARCHAR(64) NOT NULL COMMENT 'Franchisee code',
  `franchisee_name` VARCHAR(120) NOT NULL COMMENT 'Franchisee name',
  `contact_name` VARCHAR(80) DEFAULT NULL COMMENT 'Contact name',
  `contact_phone` VARCHAR(30) DEFAULT NULL COMMENT 'Contact phone',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1 enabled, 0 disabled',
  `remark` VARCHAR(255) DEFAULT NULL COMMENT 'Remark',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_franchisee_code` (`franchisee_code`),
  KEY `idx_franchisee_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Franchisee tenant';

INSERT INTO `franchisee` (`id`, `franchisee_code`, `franchisee_name`, `status`, `remark`)
VALUES (1, 'DIRECT', '直营总部', 1, 'Default direct tenant')
ON DUPLICATE KEY UPDATE
  `franchisee_name` = '直营总部',
  `status` = 1;

ALTER TABLE `store`
  ADD COLUMN `franchisee_id` BIGINT NOT NULL DEFAULT 1 COMMENT 'Franchisee tenant ID' AFTER `id`;

ALTER TABLE `store`
  ADD KEY `idx_store_franchisee` (`franchisee_id`);

CREATE TABLE IF NOT EXISTS `mini_admin_staff` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `franchisee_id` BIGINT NOT NULL DEFAULT 1 COMMENT 'Franchisee tenant ID',
  `openid` VARCHAR(128) NOT NULL COMMENT 'Wechat openid',
  `staff_no` VARCHAR(64) NOT NULL COMMENT 'Staff number',
  `staff_name` VARCHAR(80) NOT NULL COMMENT 'Staff name',
  `mobile` VARCHAR(30) DEFAULT NULL COMMENT 'Mobile phone',
  `role_code` VARCHAR(40) NOT NULL DEFAULT 'store_staff' COMMENT 'platform_admin/franchisee_owner/store_manager/store_staff/finance/operator',
  `data_scope` VARCHAR(30) NOT NULL DEFAULT 'store' COMMENT 'platform/franchisee/store',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1 enabled, 0 disabled',
  `last_login_time` DATETIME DEFAULT NULL COMMENT 'Last login time',
  `remark` VARCHAR(255) DEFAULT NULL COMMENT 'Remark',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mini_admin_openid` (`openid`),
  UNIQUE KEY `uk_mini_admin_staff_no` (`staff_no`),
  KEY `idx_mini_admin_franchisee` (`franchisee_id`),
  KEY `idx_mini_admin_role` (`role_code`),
  KEY `idx_mini_admin_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Mini admin staff';

CREATE TABLE IF NOT EXISTS `mini_admin_staff_store` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `staff_id` BIGINT NOT NULL COMMENT 'Staff ID',
  `store_id` BIGINT NOT NULL COMMENT 'Store ID',
  `is_primary` TINYINT NOT NULL DEFAULT 0 COMMENT 'Primary store flag',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_staff_store` (`staff_id`, `store_id`),
  KEY `idx_staff_store_staff` (`staff_id`),
  KEY `idx_staff_store_store` (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Mini admin staff-store scope';

CREATE TABLE IF NOT EXISTS `mini_admin_staff_session` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `staff_id` BIGINT NOT NULL COMMENT 'Staff ID',
  `openid` VARCHAR(128) NOT NULL COMMENT 'Wechat openid',
  `token` VARCHAR(96) NOT NULL COMMENT 'Session token',
  `expire_time` DATETIME NOT NULL COMMENT 'Expire time',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mini_admin_session_token` (`token`),
  KEY `idx_mini_admin_session_staff` (`staff_id`),
  KEY `idx_mini_admin_session_expire` (`expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Mini admin login session';

INSERT INTO `mini_admin_staff` (
  `id`,
  `franchisee_id`,
  `openid`,
  `staff_no`,
  `staff_name`,
  `role_code`,
  `data_scope`,
  `status`,
  `remark`
)
VALUES (
  1,
  1,
  'mock_openid_local',
  'A000001',
  '本地测试店长',
  'store_manager',
  'store',
  1,
  'Seed store manager account for local mini-admin login when WeChat credentials are not configured'
)
ON DUPLICATE KEY UPDATE
  `staff_name` = '本地测试店长',
  `role_code` = 'store_manager',
  `data_scope` = 'store',
  `status` = 1;

INSERT INTO `mini_admin_staff_store` (`staff_id`, `store_id`, `is_primary`, `created_at`)
SELECT 1, s.`id`, 1, NOW()
FROM `store` s
WHERE s.`id` = (SELECT MIN(`id`) FROM `store`)
  AND NOT EXISTS (
    SELECT 1
    FROM `mini_admin_staff_store` ss
    WHERE ss.`staff_id` = 1
      AND ss.`store_id` = s.`id`
  );

UPDATE `mini_admin_staff_store` ss
JOIN `store` s ON s.`id` = ss.`store_id`
SET ss.`is_primary` = 1
WHERE ss.`staff_id` = 1
  AND s.`id` = (SELECT MIN(`id`) FROM `store`);

INSERT INTO `mini_admin_staff` (
  `id`, `franchisee_id`, `openid`, `staff_no`, `staff_name`, `role_code`, `data_scope`, `status`, `remark`
)
VALUES (
  101, 1, 'mock_openid_platform', 'A_PLATFORM', '总部测试管理员', 'platform_admin', 'platform', 1, 'Local demo platform admin'
)
ON DUPLICATE KEY UPDATE
  `staff_name` = '总部测试管理员',
  `role_code` = 'platform_admin',
  `data_scope` = 'platform',
  `status` = 1;

INSERT INTO `mini_admin_staff` (
  `id`, `franchisee_id`, `openid`, `staff_no`, `staff_name`, `role_code`, `data_scope`, `status`, `remark`
)
VALUES (
  102, 1, 'mock_openid_franchisee', 'A_FRANCHISEE', '加盟老板演示号', 'franchisee_owner', 'franchisee', 1, 'Local demo franchisee owner'
)
ON DUPLICATE KEY UPDATE
  `staff_name` = '加盟老板演示号',
  `role_code` = 'franchisee_owner',
  `data_scope` = 'franchisee',
  `franchisee_id` = 1,
  `status` = 1;

INSERT INTO `mini_admin_staff` (
  `id`, `franchisee_id`, `openid`, `staff_no`, `staff_name`, `role_code`, `data_scope`, `status`, `remark`
)
VALUES (
  103, 1, 'mock_openid_store', 'A_STORE', '门店一店长演示号', 'store_manager', 'store', 1, 'Local demo store manager'
)
ON DUPLICATE KEY UPDATE
  `staff_name` = '门店一店长演示号',
  `role_code` = 'store_manager',
  `data_scope` = 'store',
  `franchisee_id` = 1,
  `status` = 1;

INSERT INTO `mini_admin_staff_store` (`staff_id`, `store_id`, `is_primary`, `created_at`)
SELECT 103, s.`id`, 1, NOW()
FROM `store` s
WHERE s.`id` = (SELECT MIN(`id`) FROM `store`)
  AND NOT EXISTS (
    SELECT 1
    FROM `mini_admin_staff_store` ss
    WHERE ss.`staff_id` = 103
      AND ss.`store_id` = s.`id`
  );
