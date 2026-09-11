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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='排行榜展示时长增加记录，不参与真实订单结算';
