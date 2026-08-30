package com.washer.backend.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseMigrationRunner implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseMigrationRunner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        ensureStoreFeatureTagsColumn();
        ensureUserPointsColumn();
        ensureMembershipSchema();
        ensurePointMallProductSchema();
        ensurePointRedemptionOrderSchema();
    }

    private void ensureStoreFeatureTagsColumn() {
        if (!tableExists("store") || columnExists("store", "feature_tags")) {
            return;
        }
        jdbcTemplate.execute(
            """
                ALTER TABLE `store`
                  ADD COLUMN `feature_tags` VARCHAR(255) DEFAULT NULL COMMENT 'Store feature tags, comma separated' AFTER `business_hours`
                """
        );
    }

    private void ensureUserPointsColumn() {
        if (!tableExists("user_info")) {
            return;
        }
        if (columnExists("user_info", "points")) {
            return;
        }
        jdbcTemplate.execute(
            """
                ALTER TABLE `user_info`
                  ADD COLUMN `points` INT NOT NULL DEFAULT 0 COMMENT '用户积分' AFTER `member_level`
                """
        );
    }

    private boolean tableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject(
            """
                SELECT COUNT(*)
                FROM information_schema.TABLES
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = ?
                """,
            Integer.class,
            tableName
        );
        return count != null && count > 0;
    }

    private boolean columnExists(String tableName, String columnName) {
        Integer count = jdbcTemplate.queryForObject(
            """
                SELECT COUNT(*)
                FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = ?
                  AND COLUMN_NAME = ?
                """,
            Integer.class,
            tableName,
            columnName
        );
        return count != null && count > 0;
    }

    private void ensureMembershipSchema() {
        if (!tableExists("user_info")) {
            return;
        }
        if (!columnExists("user_info", "member_expire_time")) {
            jdbcTemplate.execute(
                """
                    ALTER TABLE `user_info`
                      ADD COLUMN `member_expire_time` DATETIME DEFAULT NULL COMMENT '会员到期时间，空表示历史永久会员' AFTER `member_since_time`
                    """
            );
        }
        jdbcTemplate.execute(
            """
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
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """
        );
        jdbcTemplate.execute(
            """
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
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """
        );
        jdbcTemplate.execute(
            """
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
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """
        );
        jdbcTemplate.update(
            """
                INSERT INTO `membership_setting` (`setting_key`, `benefit_text`)
                SELECT 'default', '会员日享首段洗车优惠，月会员和年会员按有效期享受会员权益'
                WHERE NOT EXISTS (
                  SELECT 1 FROM `membership_setting` WHERE `setting_key` = 'default'
                )
                """
        );
        jdbcTemplate.update(
            """
                INSERT INTO `membership_plan` (`plan_code`, `plan_name`, `plan_type`, `duration_months`, `price`, `benefit_text`, `sort_order`)
                SELECT 'monthly', '月会员', 'monthly', 1, 19.90, '开通后 1 个月享受会员日优惠和会员价', 10
                WHERE NOT EXISTS (
                  SELECT 1 FROM `membership_plan` WHERE `plan_code` = 'monthly'
                )
                """
        );
        jdbcTemplate.update(
            """
                INSERT INTO `membership_plan` (`plan_code`, `plan_name`, `plan_type`, `duration_months`, `price`, `benefit_text`, `sort_order`)
                SELECT 'yearly', '年会员', 'yearly', 12, 199.00, '开通后 12 个月享受会员日优惠和会员价', 20
                WHERE NOT EXISTS (
                  SELECT 1 FROM `membership_plan` WHERE `plan_code` = 'yearly'
                )
                """
        );
    }

    private void ensurePointMallProductSchema() {
        jdbcTemplate.execute(
            """
                CREATE TABLE IF NOT EXISTS `point_mall_product` (
                  `id` BIGINT NOT NULL AUTO_INCREMENT,
                  `title` VARCHAR(100) NOT NULL,
                  `description` VARCHAR(500) DEFAULT NULL,
                  `cover_image` VARCHAR(500) DEFAULT NULL,
                  `product_type` VARCHAR(20) NOT NULL DEFAULT 'wash_service',
                  `points_price` INT NOT NULL,
                  `stock_total` INT NOT NULL DEFAULT 0,
                  `limit_per_user` INT NOT NULL DEFAULT 0,
                  `effective_time` DATETIME DEFAULT NULL,
                  `expire_time` DATETIME DEFAULT NULL,
                  `status` TINYINT NOT NULL DEFAULT 0,
                  `sort_order` INT NOT NULL DEFAULT 0,
                  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                  PRIMARY KEY (`id`),
                  KEY `idx_point_mall_product_status` (`status`),
                  KEY `idx_point_mall_product_available` (`effective_time`, `expire_time`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """
        );
    }

    private void ensurePointRedemptionOrderSchema() {
        jdbcTemplate.execute(
            """
                CREATE TABLE IF NOT EXISTS `point_redemption_order` (
                  `id` BIGINT NOT NULL AUTO_INCREMENT,
                  `redemption_no` VARCHAR(64) NOT NULL,
                  `request_no` VARCHAR(64) DEFAULT NULL,
                  `user_id` BIGINT NOT NULL,
                  `product_id` BIGINT NOT NULL,
                  `product_title_snapshot` VARCHAR(100) NOT NULL,
                  `points_amount` INT NOT NULL,
                  `fulfillment_status` VARCHAR(20) NOT NULL DEFAULT 'pending',
                  `fulfillment_reference` VARCHAR(128) DEFAULT NULL,
                  `remark` VARCHAR(255) DEFAULT NULL,
                  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                  PRIMARY KEY (`id`),
                  UNIQUE KEY `uk_point_redemption_no` (`redemption_no`),
                  UNIQUE KEY `uk_point_redemption_user_request` (`user_id`, `request_no`),
                  KEY `idx_point_redemption_user_created` (`user_id`, `created_at`),
                  KEY `idx_point_redemption_product` (`product_id`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """
        );
    }
}
