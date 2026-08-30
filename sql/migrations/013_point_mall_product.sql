SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `point_mall_product` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `title` VARCHAR(100) NOT NULL COMMENT '商品名称',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '商品说明',
  `cover_image` VARCHAR(500) DEFAULT NULL COMMENT '商品封面图片URL',
  `product_type` VARCHAR(20) NOT NULL DEFAULT 'wash_service' COMMENT '商品类型：wash_service/coupon/physical',
  `points_price` INT NOT NULL COMMENT '兑换所需积分',
  `stock_total` INT NOT NULL DEFAULT 0 COMMENT '可兑换库存',
  `limit_per_user` INT NOT NULL DEFAULT 0 COMMENT '每用户限兑次数，0表示不限',
  `effective_time` DATETIME DEFAULT NULL COMMENT '上架生效时间，空表示立即生效',
  `expire_time` DATETIME DEFAULT NULL COMMENT '下架失效时间，空表示长期有效',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态：1上架 0下架',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序值，越小越靠前',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_point_mall_product_status` (`status`),
  KEY `idx_point_mall_product_available` (`effective_time`, `expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='积分商城商品表';
