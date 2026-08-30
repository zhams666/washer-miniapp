SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `wash_queue` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `queue_no` VARCHAR(64) NOT NULL COMMENT 'Queue number',
  `user_id` BIGINT NOT NULL COMMENT 'User ID',
  `store_id` BIGINT NOT NULL COMMENT 'Store ID',
  `queue_status` VARCHAR(20) NOT NULL DEFAULT 'waiting' COMMENT 'waiting/cancelled/called',
  `user_latitude` DECIMAL(10,6) DEFAULT NULL COMMENT 'Last user latitude',
  `user_longitude` DECIMAL(10,6) DEFAULT NULL COMMENT 'Last user longitude',
  `distance_km` DECIMAL(10,3) DEFAULT NULL COMMENT 'Last distance to store in km',
  `cancel_reason` VARCHAR(120) DEFAULT NULL COMMENT 'Cancel reason',
  `queued_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Queued time',
  `cancelled_at` DATETIME DEFAULT NULL COMMENT 'Cancelled time',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_queue_no` (`queue_no`),
  KEY `idx_queue_store_status` (`store_id`, `queue_status`, `id`),
  KEY `idx_queue_user_store_status` (`user_id`, `store_id`, `queue_status`, `id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Wash queue';
