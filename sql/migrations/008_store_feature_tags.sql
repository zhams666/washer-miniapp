SET NAMES utf8mb4;

ALTER TABLE `store`
  ADD COLUMN `feature_tags` VARCHAR(255) DEFAULT NULL COMMENT 'Store feature tags, comma separated' AFTER `business_hours`;
