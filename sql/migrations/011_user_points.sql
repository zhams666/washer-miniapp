ALTER TABLE `user_info`
  ADD COLUMN `points` INT NOT NULL DEFAULT 0 COMMENT '用户积分' AFTER `member_level`;
