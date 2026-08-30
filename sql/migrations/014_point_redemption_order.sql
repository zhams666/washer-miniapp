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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='积分兑换订单';
