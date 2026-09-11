ALTER TABLE `store`
  ADD COLUMN `cover_image` VARCHAR(500) DEFAULT NULL COMMENT '门店封面图片' AFTER `feature_tags`,
  ADD COLUMN `door_close_interval_one_start` INT DEFAULT NULL COMMENT '洗车自动关门区间一开始分钟' AFTER `cover_image`,
  ADD COLUMN `door_close_interval_one_end` INT DEFAULT NULL COMMENT '洗车自动关门区间一结束分钟' AFTER `door_close_interval_one_start`,
  ADD COLUMN `door_close_interval_two_start` INT DEFAULT NULL COMMENT '洗车自动关门区间二开始分钟' AFTER `door_close_interval_one_end`,
  ADD COLUMN `door_close_interval_two_end` INT DEFAULT NULL COMMENT '洗车自动关门区间二结束分钟' AFTER `door_close_interval_two_start`,
  ADD COLUMN `register_reward_amount` DECIMAL(12,2) DEFAULT NULL COMMENT '注册奖励金' AFTER `door_close_interval_two_end`,
  ADD COLUMN `invite_reward_amount` DECIMAL(12,2) DEFAULT NULL COMMENT '邀请奖励金' AFTER `register_reward_amount`,
  ADD COLUMN `activity_intro` VARCHAR(1000) DEFAULT NULL COMMENT '活动介绍' AFTER `invite_reward_amount`,
  ADD COLUMN `recharge_description` VARCHAR(1000) DEFAULT NULL COMMENT '充值说明' AFTER `activity_intro`,
  ADD COLUMN `member_description` VARCHAR(1000) DEFAULT NULL COMMENT '会员说明' AFTER `recharge_description`,
  ADD COLUMN `cabinet_min_recharge_amount` DECIMAL(12,2) DEFAULT NULL COMMENT '开柜最低单充金额' AFTER `member_description`,
  ADD COLUMN `cabinet_min_balance_amount` DECIMAL(12,2) DEFAULT NULL COMMENT '开柜最低剩余余额' AFTER `cabinet_min_recharge_amount`;
