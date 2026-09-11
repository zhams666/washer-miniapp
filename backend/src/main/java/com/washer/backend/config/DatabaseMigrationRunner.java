package com.washer.backend.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!cloudbase")
public class DatabaseMigrationRunner implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseMigrationRunner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        ensureStoreFeatureTagsColumn();
        ensureStoreManagementSettingColumns();
        ensureUserPointsColumn();
        ensureMembershipSchema();
        ensurePointMallProductSchema();
        ensurePointRedemptionOrderSchema();
        ensureRankingDisplayAdjustmentSchema();
        ensureStoreExchangeVoucherSchema();
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

    private boolean indexExists(String tableName, String indexName) {
        Integer count = jdbcTemplate.queryForObject(
            """
                SELECT COUNT(*)
                FROM information_schema.STATISTICS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = ?
                  AND INDEX_NAME = ?
                """,
            Integer.class,
            tableName,
            indexName
        );
        return count != null && count > 0;
    }

    private void ensureStoreManagementSettingColumns() {
        if (!tableExists("store")) {
            return;
        }
        if (!columnExists("store", "cover_image")) {
            jdbcTemplate.execute(
                """
                    ALTER TABLE `store`
                      ADD COLUMN `cover_image` VARCHAR(500) DEFAULT NULL COMMENT '门店封面图片' AFTER `feature_tags`
                    """
            );
        }
        if (!columnExists("store", "door_close_interval_one_start")) {
            jdbcTemplate.execute(
                """
                    ALTER TABLE `store`
                      ADD COLUMN `door_close_interval_one_start` INT DEFAULT NULL COMMENT '洗车自动关门区间一开始分钟' AFTER `cover_image`
                    """
            );
        }
        if (!columnExists("store", "door_close_interval_one_end")) {
            jdbcTemplate.execute(
                """
                    ALTER TABLE `store`
                      ADD COLUMN `door_close_interval_one_end` INT DEFAULT NULL COMMENT '洗车自动关门区间一结束分钟' AFTER `door_close_interval_one_start`
                    """
            );
        }
        if (!columnExists("store", "door_close_interval_two_start")) {
            jdbcTemplate.execute(
                """
                    ALTER TABLE `store`
                      ADD COLUMN `door_close_interval_two_start` INT DEFAULT NULL COMMENT '洗车自动关门区间二开始分钟' AFTER `door_close_interval_one_end`
                    """
            );
        }
        if (!columnExists("store", "door_close_interval_two_end")) {
            jdbcTemplate.execute(
                """
                    ALTER TABLE `store`
                      ADD COLUMN `door_close_interval_two_end` INT DEFAULT NULL COMMENT '洗车自动关门区间二结束分钟' AFTER `door_close_interval_two_start`
                    """
            );
        }
        if (!columnExists("store", "register_reward_amount")) {
            jdbcTemplate.execute(
                """
                    ALTER TABLE `store`
                      ADD COLUMN `register_reward_amount` DECIMAL(12,2) DEFAULT NULL COMMENT '注册奖励金' AFTER `door_close_interval_two_end`
                    """
            );
        }
        if (!columnExists("store", "invite_reward_amount")) {
            jdbcTemplate.execute(
                """
                    ALTER TABLE `store`
                      ADD COLUMN `invite_reward_amount` DECIMAL(12,2) DEFAULT NULL COMMENT '邀请奖励金' AFTER `register_reward_amount`
                    """
            );
        }
        if (!columnExists("store", "activity_intro")) {
            jdbcTemplate.execute(
                """
                    ALTER TABLE `store`
                      ADD COLUMN `activity_intro` VARCHAR(1000) DEFAULT NULL COMMENT '活动介绍' AFTER `invite_reward_amount`
                    """
            );
        }
        if (!columnExists("store", "recharge_description")) {
            jdbcTemplate.execute(
                """
                    ALTER TABLE `store`
                      ADD COLUMN `recharge_description` VARCHAR(1000) DEFAULT NULL COMMENT '充值说明' AFTER `activity_intro`
                    """
            );
        }
        if (!columnExists("store", "member_description")) {
            jdbcTemplate.execute(
                """
                    ALTER TABLE `store`
                      ADD COLUMN `member_description` VARCHAR(1000) DEFAULT NULL COMMENT '会员说明' AFTER `recharge_description`
                    """
            );
        }
        if (!columnExists("store", "cabinet_min_recharge_amount")) {
            jdbcTemplate.execute(
                """
                    ALTER TABLE `store`
                      ADD COLUMN `cabinet_min_recharge_amount` DECIMAL(12,2) DEFAULT NULL COMMENT '开柜最低单充金额' AFTER `member_description`
                    """
            );
        }
        if (!columnExists("store", "cabinet_min_balance_amount")) {
            jdbcTemplate.execute(
                """
                    ALTER TABLE `store`
                      ADD COLUMN `cabinet_min_balance_amount` DECIMAL(12,2) DEFAULT NULL COMMENT '开柜最低剩余余额' AFTER `cabinet_min_recharge_amount`
                    """
            );
        }
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

    private void ensureRankingDisplayAdjustmentSchema() {
        jdbcTemplate.execute(
            """
                CREATE TABLE IF NOT EXISTS `ranking_display_adjustment` (
                  `id` BIGINT NOT NULL AUTO_INCREMENT,
                  `user_id` BIGINT NOT NULL,
                  `scope` VARCHAR(20) NOT NULL DEFAULT 'total',
                  `display_duration_seconds` BIGINT NOT NULL,
                  `remark` VARCHAR(255) DEFAULT NULL,
                  `occurred_at` DATETIME NOT NULL,
                  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                  PRIMARY KEY (`id`),
                  KEY `idx_ranking_display_adjustment_occurred` (`occurred_at`),
                  KEY `idx_ranking_display_adjustment_scope_occurred` (`scope`, `occurred_at`),
                  KEY `idx_ranking_display_adjustment_scope_user_occurred` (`scope`, `user_id`, `occurred_at`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """
        );
        if (!columnExists("ranking_display_adjustment", "scope")) {
            jdbcTemplate.execute(
                """
                    ALTER TABLE `ranking_display_adjustment`
                      ADD COLUMN `scope` VARCHAR(20) NOT NULL DEFAULT 'total' AFTER `user_id`
                    """
            );
        }
        if (!indexExists("ranking_display_adjustment", "idx_ranking_display_adjustment_scope_occurred")) {
            jdbcTemplate.execute(
                """
                    CREATE INDEX `idx_ranking_display_adjustment_scope_occurred`
                      ON `ranking_display_adjustment` (`scope`, `occurred_at`)
                    """
            );
        }
        if (!indexExists("ranking_display_adjustment", "idx_ranking_display_adjustment_scope_user_occurred")) {
            jdbcTemplate.execute(
                """
                    CREATE INDEX `idx_ranking_display_adjustment_scope_user_occurred`
                      ON `ranking_display_adjustment` (`scope`, `user_id`, `occurred_at`)
                    """
            );
        }
    }

    private void ensureStoreExchangeVoucherSchema() {
        jdbcTemplate.execute(
            """
                CREATE TABLE IF NOT EXISTS `store_exchange_voucher` (
                  `id` BIGINT NOT NULL AUTO_INCREMENT,
                  `batch_no` VARCHAR(64) NOT NULL,
                  `serial_no` VARCHAR(64) NOT NULL,
                  `store_id` BIGINT NOT NULL,
                  `amount` DECIMAL(12,2) NOT NULL DEFAULT 0.00,
                  `status` VARCHAR(20) NOT NULL DEFAULT 'unused',
                  `redeemed_user_id` BIGINT DEFAULT NULL,
                  `redeemed_wallet_transaction_id` BIGINT DEFAULT NULL,
                  `created_by_staff_id` BIGINT DEFAULT NULL,
                  `created_by_role_code` VARCHAR(40) DEFAULT NULL,
                  `redeemed_at` DATETIME DEFAULT NULL,
                  `remark` VARCHAR(255) DEFAULT NULL,
                  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                  PRIMARY KEY (`id`),
                  UNIQUE KEY `uk_store_exchange_voucher_serial` (`serial_no`),
                  KEY `idx_store_exchange_voucher_batch` (`batch_no`),
                  KEY `idx_store_exchange_voucher_store_status` (`store_id`, `status`, `created_at`),
                  KEY `idx_store_exchange_voucher_redeemed_user` (`redeemed_user_id`, `redeemed_at`),
                  KEY `idx_store_exchange_voucher_transaction` (`redeemed_wallet_transaction_id`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='门店兑换券码'
                """
        );
    }
}
