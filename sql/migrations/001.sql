

-- =========================
-- 1. 用户基础模块
-- =========================

CREATE TABLE IF NOT EXISTS `user_info` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_no` VARCHAR(64) DEFAULT NULL COMMENT '用户编号',
  `openid` VARCHAR(128) DEFAULT NULL COMMENT '微信小程序openid',
  `unionid` VARCHAR(128) DEFAULT NULL COMMENT '微信unionid',
  `nickname` VARCHAR(100) NOT NULL DEFAULT '微信用户' COMMENT '昵称',
  `real_name` VARCHAR(50) DEFAULT NULL COMMENT '真实姓名',
  `mobile` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
  `avatar_url` VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
  `user_status` TINYINT NOT NULL DEFAULT 1 COMMENT '用户状态：1正常 0禁用',
  `register_source` VARCHAR(20) NOT NULL DEFAULT 'miniapp' COMMENT '注册来源：miniapp/admin/import',
  `is_member` TINYINT NOT NULL DEFAULT 0 COMMENT '是否会员：1是 0否',
  `member_level` VARCHAR(30) NOT NULL DEFAULT 'normal' COMMENT '会员等级：normal/silver/gold/platinum',
  `member_since_time` DATETIME DEFAULT NULL COMMENT '成为会员时间',
  `last_login_time` DATETIME DEFAULT NULL COMMENT '最后登录时间',
  `last_consume_time` DATETIME DEFAULT NULL COMMENT '最后消费时间',
  `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_no` (`user_no`),
  UNIQUE KEY `uk_openid` (`openid`),
  UNIQUE KEY `uk_unionid` (`unionid`),
  UNIQUE KEY `uk_mobile` (`mobile`),
  KEY `idx_user_status` (`user_status`),
  KEY `idx_is_member` (`is_member`),
  KEY `idx_member_level` (`member_level`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户基础信息表'

CREATE TABLE IF NOT EXISTS `user_membership_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `before_is_member` TINYINT NOT NULL DEFAULT 0 COMMENT '变更前是否会员',
  `after_is_member` TINYINT NOT NULL DEFAULT 0 COMMENT '变更后是否会员',
  `before_member_level` VARCHAR(30) DEFAULT NULL COMMENT '变更前会员等级',
  `after_member_level` VARCHAR(30) DEFAULT NULL COMMENT '变更后会员等级',
  `change_type` VARCHAR(30) NOT NULL COMMENT '变更类型：recharge/open/manual/close/adjust',
  `change_reason` VARCHAR(255) DEFAULT NULL COMMENT '变更原因',
  `operator_type` VARCHAR(20) NOT NULL DEFAULT 'system' COMMENT '操作人类型：system/admin/user',
  `operator_id` BIGINT DEFAULT NULL COMMENT '操作人ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_change_type` (`change_type`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户会员变更记录表';

CREATE TABLE IF NOT EXISTS `user_vehicle` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `plate_no` VARCHAR(20) DEFAULT NULL COMMENT '车牌号',
  `vehicle_brand` VARCHAR(50) DEFAULT NULL COMMENT '品牌',
  `vehicle_model` VARCHAR(100) DEFAULT NULL COMMENT '车型',
  `vehicle_color` VARCHAR(30) DEFAULT NULL COMMENT '车身颜色',
  `is_default` TINYINT NOT NULL DEFAULT 0 COMMENT '是否默认车辆：1是 0否',
  `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_plate_no` (`plate_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户车辆信息表';

-- =========================
-- 2. 门店 / 工位 / 设备模块
-- =========================

CREATE TABLE IF NOT EXISTS `store` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `store_code` VARCHAR(64) NOT NULL COMMENT '门店编码',
  `store_name` VARCHAR(100) NOT NULL COMMENT '门店名称',
  `store_status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1营业中 0停用',
  `province` VARCHAR(50) DEFAULT NULL COMMENT '省',
  `city` VARCHAR(50) DEFAULT NULL COMMENT '市',
  `district` VARCHAR(50) DEFAULT NULL COMMENT '区',
  `address` VARCHAR(255) DEFAULT NULL COMMENT '详细地址',
  `longitude` DECIMAL(10,6) DEFAULT NULL COMMENT '经度',
  `latitude` DECIMAL(10,6) DEFAULT NULL COMMENT '纬度',
  `contact_name` VARCHAR(50) DEFAULT NULL COMMENT '联系人',
  `contact_phone` VARCHAR(30) DEFAULT NULL COMMENT '联系电话',
  `business_hours` VARCHAR(100) DEFAULT NULL COMMENT '营业时间',
  `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_store_code` (`store_code`),
  KEY `idx_store_status` (`store_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='门店表';

CREATE TABLE IF NOT EXISTS `store_bay` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `store_id` BIGINT NOT NULL COMMENT '门店ID',
  `bay_code` VARCHAR(64) NOT NULL COMMENT '工位编码',
  `bay_name` VARCHAR(100) NOT NULL COMMENT '工位名称',
  `bay_status` VARCHAR(20) NOT NULL DEFAULT 'idle' COMMENT '工位状态：idle/occupied/offline/disabled',
  `sort_no` INT NOT NULL DEFAULT 0 COMMENT '排序',
  `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_store_bay_code` (`store_id`, `bay_code`),
  KEY `idx_store_id` (`store_id`),
  KEY `idx_bay_status` (`bay_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='门店工位表';

CREATE TABLE IF NOT EXISTS `device` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `device_code` VARCHAR(64) NOT NULL COMMENT '设备编码/序列号',
  `store_id` BIGINT NOT NULL COMMENT '所属门店ID',
  `bay_id` BIGINT DEFAULT NULL COMMENT '所属工位ID',
  `device_type` VARCHAR(30) NOT NULL DEFAULT 'washer' COMMENT '设备类型：washer/controller/gateway',
  `device_name` VARCHAR(100) DEFAULT NULL COMMENT '设备名称',
  `device_status` VARCHAR(20) NOT NULL DEFAULT 'offline' COMMENT '设备状态：offline/idle/running/paused/fault/disabled',
  `protocol_type` VARCHAR(30) DEFAULT NULL COMMENT '协议类型',
  `firmware_version` VARCHAR(50) DEFAULT NULL COMMENT '固件版本',
  `last_heartbeat_time` DATETIME DEFAULT NULL COMMENT '最后心跳时间',
  `last_online_time` DATETIME DEFAULT NULL COMMENT '最后上线时间',
  `install_time` DATETIME DEFAULT NULL COMMENT '安装时间',
  `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_device_code` (`device_code`),
  KEY `idx_store_id` (`store_id`),
  KEY `idx_bay_id` (`bay_id`),
  KEY `idx_device_status` (`device_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备表';

CREATE TABLE IF NOT EXISTS `device_heartbeat_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `device_id` BIGINT NOT NULL COMMENT '设备ID',
  `device_code` VARCHAR(64) NOT NULL COMMENT '设备编码',
  `heartbeat_time` DATETIME NOT NULL COMMENT '心跳时间',
  `device_status` VARCHAR(20) DEFAULT NULL COMMENT '设备状态快照',
  `payload` TEXT DEFAULT NULL COMMENT '心跳原始数据',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_device_id` (`device_id`),
  KEY `idx_device_code` (`device_code`),
  KEY `idx_heartbeat_time` (`heartbeat_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备心跳日志表';

CREATE TABLE IF NOT EXISTS `device_command_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `command_no` VARCHAR(64) NOT NULL COMMENT '指令号',
  `device_id` BIGINT NOT NULL COMMENT '设备ID',
  `device_code` VARCHAR(64) NOT NULL COMMENT '设备编码',
  `order_id` BIGINT DEFAULT NULL COMMENT '关联订单ID',
  `order_no` VARCHAR(64) DEFAULT NULL COMMENT '关联订单号',
  `command_type` VARCHAR(30) NOT NULL COMMENT '指令类型：start/pause/resume/stop/cancel/query_status',
  `request_payload` TEXT DEFAULT NULL COMMENT '请求报文',
  `response_payload` TEXT DEFAULT NULL COMMENT '响应报文',
  `command_status` VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '状态：pending/success/failed/timeout',
  `request_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '请求时间',
  `response_time` DATETIME DEFAULT NULL COMMENT '响应时间',
  `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_command_no` (`command_no`),
  KEY `idx_device_id` (`device_id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_command_type` (`command_type`),
  KEY `idx_command_status` (`command_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备指令日志表';

-- =========================
-- 3. 计费与优惠模块
-- =========================

CREATE TABLE IF NOT EXISTS `pricing_rule` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `store_id` BIGINT NOT NULL COMMENT '门店ID',
  `rule_name` VARCHAR(100) NOT NULL COMMENT '规则名称',
  `member_price_enabled` TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用会员价：1启用 0关闭',
  `free_minutes` INT NOT NULL DEFAULT 0 COMMENT '免费分钟数',
  `first_period_minutes` INT NOT NULL DEFAULT 0 COMMENT '首段分钟数',
  `first_period_price` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '首段价格（普通）',
  `extra_price_per_minute` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '超出后每分钟价格（普通）',
  `member_first_period_price` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '首段价格（会员）',
  `member_extra_price_per_minute` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '超出后每分钟价格（会员）',
  `first_period_discount_limit_type` VARCHAR(30) NOT NULL DEFAULT 'once_per_day' COMMENT '首段优惠限制：none/once_per_order/once_per_day/once_per_day_per_store',
  `is_default` TINYINT NOT NULL DEFAULT 0 COMMENT '是否默认规则：1是 0否',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1启用 0停用',
  `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_store_id` (`store_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='计费规则表';

CREATE TABLE IF NOT EXISTS `user_daily_discount_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `discount_date` DATE NOT NULL COMMENT '优惠日期',
  `discount_type` VARCHAR(30) NOT NULL COMMENT '优惠类型：first_period_discount',
  `store_id` BIGINT NOT NULL COMMENT '实际使用优惠的门店ID',
  `order_id` BIGINT NOT NULL COMMENT '关联订单ID',
  `order_no` VARCHAR(64) NOT NULL COMMENT '关联订单号',
  `discount_amount` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '优惠金额',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_date_type` (`user_id`, `discount_date`, `discount_type`),
  KEY `idx_store_id` (`store_id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_discount_date` (`discount_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户每日优惠记录表（全平台每日一次）';

-- =========================
-- 4. 钱包模块
-- =========================

CREATE TABLE IF NOT EXISTS `user_store_wallet` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `store_id` BIGINT NOT NULL COMMENT '充值归属门店ID',
  `principal_balance` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '本金余额（可跨店使用，可退款）',
  `gift_balance` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '赠送余额（仅本店使用，不可退款）',
  `total_recharge_principal` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '累计充值本金',
  `total_recharge_gift` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '累计赠送金额',
  `total_consume_principal` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '累计消费本金',
  `total_consume_gift` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '累计消费赠送',
  `total_refund_principal` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '累计退款本金',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1正常 0冻结',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_store` (`user_id`, `store_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_store_id` (`store_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户个人钱包表（按门店归属）';

CREATE TABLE IF NOT EXISTS `wallet_transaction` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `transaction_no` VARCHAR(64) NOT NULL COMMENT '流水单号',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `store_id` BIGINT NOT NULL COMMENT '归属门店ID',
  `biz_type` VARCHAR(30) NOT NULL COMMENT '业务类型：recharge/consume/refund/adjust/clear_gift',
  `amount_type` VARCHAR(20) NOT NULL COMMENT '金额类型：principal/gift',
  `change_type` VARCHAR(10) NOT NULL COMMENT '变动类型：in/out',
  `amount` DECIMAL(10,2) NOT NULL COMMENT '变动金额',
  `balance_before` DECIMAL(10,2) NOT NULL COMMENT '变动前余额',
  `balance_after` DECIMAL(10,2) NOT NULL COMMENT '变动后余额',
  `related_order_id` BIGINT DEFAULT NULL COMMENT '关联订单ID',
  `related_order_no` VARCHAR(64) DEFAULT NULL COMMENT '关联订单号',
  `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_transaction_no` (`transaction_no`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_store_id` (`store_id`),
  KEY `idx_related_order_id` (`related_order_id`),
  KEY `idx_biz_type` (`biz_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='钱包流水表';

CREATE TABLE IF NOT EXISTS `recharge_order` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `recharge_order_no` VARCHAR(64) NOT NULL COMMENT '充值订单号',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `store_id` BIGINT NOT NULL COMMENT '充值门店ID',
  `principal_amount` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '本金金额',
  `gift_amount` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '赠送金额',
  `pay_amount` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '实际支付金额',
  `pay_channel` VARCHAR(30) DEFAULT NULL COMMENT '支付渠道：wxpay/alipay/cash/manual',
  `pay_status` VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '支付状态：pending/paid/failed/closed/refunded',
  `pay_time` DATETIME DEFAULT NULL COMMENT '支付时间',
  `third_party_trade_no` VARCHAR(64) DEFAULT NULL COMMENT '第三方流水号',
  `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_recharge_order_no` (`recharge_order_no`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_store_id` (`store_id`),
  KEY `idx_pay_status` (`pay_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='充值订单表';

-- =========================
-- 5. 次卡模块
-- =========================

CREATE TABLE IF NOT EXISTS `card_product` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `store_id` BIGINT NOT NULL COMMENT '所属门店ID',
  `card_name` VARCHAR(100) NOT NULL COMMENT '次卡名称',
  `card_type` VARCHAR(20) NOT NULL COMMENT '次卡类型：store/meituan/douyin',
  `total_times` INT NOT NULL COMMENT '总次数',
  `sale_price` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '销售金额',
  `valid_days` INT DEFAULT NULL COMMENT '有效天数，空表示长期有效',
  `is_new_user_only` TINYINT NOT NULL DEFAULT 0 COMMENT '是否仅新用户可购买：1是 0否',
  `purchase_limit` INT NOT NULL DEFAULT 1 COMMENT '购买次数限制，0表示不限制',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1上架 0下架',
  `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_store_id` (`store_id`),
  KEY `idx_card_type` (`card_type`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='次卡商品表';

CREATE TABLE IF NOT EXISTS `card_purchase_order` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `purchase_order_no` VARCHAR(64) NOT NULL COMMENT '购买单号',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `store_id` BIGINT NOT NULL COMMENT '门店ID',
  `card_product_id` BIGINT NOT NULL COMMENT '次卡商品ID',
  `card_type` VARCHAR(20) NOT NULL COMMENT '次卡类型：store/meituan/douyin',
  `source_channel` VARCHAR(20) NOT NULL COMMENT '购买渠道：store/meituan/douyin',
  `buy_count` INT NOT NULL DEFAULT 1 COMMENT '购买数量',
  `pay_amount` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '支付金额',
  `pay_status` VARCHAR(20) NOT NULL DEFAULT 'paid' COMMENT '支付状态：pending/paid/failed/cancelled',
  `purchase_time` DATETIME NOT NULL COMMENT '购买时间',
  `external_order_no` VARCHAR(64) DEFAULT NULL COMMENT '外部平台订单号',
  `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_purchase_order_no` (`purchase_order_no`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_store_id` (`store_id`),
  KEY `idx_card_product_id` (`card_product_id`),
  KEY `idx_source_channel` (`source_channel`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='次卡购买订单表';

CREATE TABLE IF NOT EXISTS `user_card` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `store_id` BIGINT NOT NULL COMMENT '绑定门店ID',
  `card_product_id` BIGINT NOT NULL COMMENT '次卡商品ID',
  `card_type` VARCHAR(20) NOT NULL COMMENT '次卡类型：store/meituan/douyin',
  `source_channel` VARCHAR(20) NOT NULL COMMENT '来源渠道：store/meituan/douyin',
  `card_no` VARCHAR(64) NOT NULL COMMENT '用户次卡编号',
  `total_times` INT NOT NULL COMMENT '总次数',
  `used_times` INT NOT NULL DEFAULT 0 COMMENT '已使用次数',
  `remaining_times` INT NOT NULL COMMENT '剩余次数',
  `purchase_time` DATETIME NOT NULL COMMENT '购买时间',
  `effective_time` DATETIME DEFAULT NULL COMMENT '生效时间',
  `expire_time` DATETIME DEFAULT NULL COMMENT '失效时间',
  `status` VARCHAR(20) NOT NULL DEFAULT 'active' COMMENT '状态：active/used_up/expired/cancelled',
  `external_order_no` VARCHAR(64) DEFAULT NULL COMMENT '外部平台订单号',
  `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_card_no` (`card_no`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_store_id` (`store_id`),
  KEY `idx_card_product_id` (`card_product_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户次卡表';

CREATE TABLE IF NOT EXISTS `card_usage_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `usage_no` VARCHAR(64) NOT NULL COMMENT '核销记录号',
  `user_card_id` BIGINT NOT NULL COMMENT '用户次卡ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `store_id` BIGINT NOT NULL COMMENT '使用门店ID',
  `order_id` BIGINT DEFAULT NULL COMMENT '关联洗车订单ID',
  `order_no` VARCHAR(64) DEFAULT NULL COMMENT '关联洗车订单号',
  `used_times` INT NOT NULL DEFAULT 1 COMMENT '本次使用次数',
  `usage_time` DATETIME NOT NULL COMMENT '消费/核销时间',
  `operator_type` VARCHAR(20) NOT NULL DEFAULT 'system' COMMENT '操作类型：system/admin/user/device',
  `operator_id` BIGINT DEFAULT NULL COMMENT '操作人ID',
  `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_usage_no` (`usage_no`),
  KEY `idx_user_card_id` (`user_card_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_store_id` (`store_id`),
  KEY `idx_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='次卡使用记录表';

-- =========================
-- 6. 订单模块
-- =========================

CREATE TABLE IF NOT EXISTS `wash_order` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `order_no` VARCHAR(64) NOT NULL COMMENT '订单号',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `store_id` BIGINT NOT NULL COMMENT '消费门店ID',
  `bay_id` BIGINT DEFAULT NULL COMMENT '工位ID',
  `device_id` BIGINT DEFAULT NULL COMMENT '设备ID',
  `pricing_rule_id` BIGINT DEFAULT NULL COMMENT '计费规则ID',
  `order_source` VARCHAR(20) NOT NULL DEFAULT 'miniapp' COMMENT '订单来源：miniapp/qrcode/admin/device',
  `order_status` VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '订单状态：pending/running/paused/completed/cancelled/abnormal/refunding/refunded',
  `device_status_snapshot` VARCHAR(20) DEFAULT NULL COMMENT '下单时设备状态快照',
  `pay_mode` VARCHAR(20) NOT NULL DEFAULT 'wallet' COMMENT '支付方式：wallet/card/mixed',
  `card_usage_id` BIGINT DEFAULT NULL COMMENT '关联次卡使用记录ID',
  `card_deduct_times` INT NOT NULL DEFAULT 0 COMMENT '本单扣减次卡次数',
  `estimated_amount` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '预估金额',
  `final_amount` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '最终金额',
  `paid_amount` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '已支付金额',
  `refund_amount` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '已退款金额',
  `is_first_period_discount_used` TINYINT NOT NULL DEFAULT 0 COMMENT '是否使用首段优惠：1是 0否',
  `first_period_discount_amount` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '首段优惠金额',
  `start_command_no` VARCHAR(64) DEFAULT NULL COMMENT '开始指令号',
  `pause_command_no` VARCHAR(64) DEFAULT NULL COMMENT '暂停指令号',
  `stop_command_no` VARCHAR(64) DEFAULT NULL COMMENT '停止指令号',
  `cancel_command_no` VARCHAR(64) DEFAULT NULL COMMENT '取消指令号',
  `start_time` DATETIME DEFAULT NULL COMMENT '开始时间',
  `pause_time` DATETIME DEFAULT NULL COMMENT '暂停时间',
  `end_time` DATETIME DEFAULT NULL COMMENT '结束时间',
  `cancel_time` DATETIME DEFAULT NULL COMMENT '取消时间',
  `settle_time` DATETIME DEFAULT NULL COMMENT '结算时间',
  `abnormal_reason` VARCHAR(255) DEFAULT NULL COMMENT '异常原因',
  `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_store_id` (`store_id`),
  KEY `idx_bay_id` (`bay_id`),
  KEY `idx_device_id` (`device_id`),
  KEY `idx_order_status` (`order_status`),
  KEY `idx_pay_mode` (`pay_mode`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='洗车订单表';

CREATE TABLE IF NOT EXISTS `wash_order_status_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `order_id` BIGINT NOT NULL COMMENT '订单ID',
  `order_no` VARCHAR(64) NOT NULL COMMENT '订单号',
  `from_status` VARCHAR(20) DEFAULT NULL COMMENT '原状态',
  `to_status` VARCHAR(20) NOT NULL COMMENT '新状态',
  `action_type` VARCHAR(30) NOT NULL COMMENT '动作：create/start/pause/resume/finish/cancel/refund/abnormal',
  `operator_type` VARCHAR(20) NOT NULL DEFAULT 'system' COMMENT '操作人类型：user/admin/system/device',
  `operator_id` BIGINT DEFAULT NULL COMMENT '操作人ID',
  `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_order_no` (`order_no`),
  KEY `idx_to_status` (`to_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单状态流转日志表';

CREATE TABLE IF NOT EXISTS `wash_order_payment_detail` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `order_id` BIGINT NOT NULL COMMENT '洗车订单ID',
  `order_no` VARCHAR(64) NOT NULL COMMENT '订单号',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `consume_store_id` BIGINT NOT NULL COMMENT '消费门店ID',
  `source_type` VARCHAR(20) NOT NULL COMMENT '来源类型：wallet/card',
  `source_store_id` BIGINT DEFAULT NULL COMMENT '来源门店ID（钱包本金归属门店/次卡绑定门店）',
  `amount_type` VARCHAR(20) DEFAULT NULL COMMENT '金额类型：principal/gift/card',
  `user_card_id` BIGINT DEFAULT NULL COMMENT '次卡ID',
  `amount` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '金额',
  `deduct_times` INT NOT NULL DEFAULT 0 COMMENT '扣减次数（次卡用）',
  `refunded_amount` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '已退款金额',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_order_no` (`order_no`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_consume_store_id` (`consume_store_id`),
  KEY `idx_source_type` (`source_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单支付明细表';

-- =========================
-- 7. 退款与结算模块
-- =========================

CREATE TABLE IF NOT EXISTS `refund_order` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `refund_order_no` VARCHAR(64) NOT NULL COMMENT '退款单号',
  `order_id` BIGINT NOT NULL COMMENT '洗车订单ID',
  `order_no` VARCHAR(64) NOT NULL COMMENT '洗车订单号',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `store_id` BIGINT NOT NULL COMMENT '消费门店ID',
  `refund_amount` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '退款金额',
  `refund_status` VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '退款状态：pending/success/failed/rejected',
  `refund_reason` VARCHAR(255) DEFAULT NULL COMMENT '退款原因',
  `refund_time` DATETIME DEFAULT NULL COMMENT '退款时间',
  `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_refund_order_no` (`refund_order_no`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_store_id` (`store_id`),
  KEY `idx_refund_status` (`refund_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='退款单表';

CREATE TABLE IF NOT EXISTS `store_settlement_detail` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `from_store_id` BIGINT NOT NULL COMMENT '应付门店ID（充值门店）',
  `to_store_id` BIGINT NOT NULL COMMENT '应收门店ID（消费门店）',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `order_id` BIGINT NOT NULL COMMENT '订单ID',
  `order_no` VARCHAR(64) NOT NULL COMMENT '订单号',
  `payment_detail_id` BIGINT NOT NULL COMMENT '支付明细ID',
  `principal_amount` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '本金消费金额',
  `refund_adjust_amount` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '退款冲减金额',
  `net_amount` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '实际结算金额',
  `biz_date` DATE NOT NULL COMMENT '业务日期',
  `detail_status` VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '状态：pending/locked/settled/cancelled',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_from_store_id` (`from_store_id`),
  KEY `idx_to_store_id` (`to_store_id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_payment_detail_id` (`payment_detail_id`),
  KEY `idx_biz_date` (`biz_date`),
  KEY `idx_detail_status` (`detail_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='门店结算明细表';
SELECT DATABASE();

ALTER TABLE `wash_order`
ADD KEY `idx_device_status` (`device_id`, `order_status`),
ADD KEY `idx_bay_status` (`bay_id`, `order_status`),
ADD KEY `idx_user_status` (`user_id`, `order_status`);


