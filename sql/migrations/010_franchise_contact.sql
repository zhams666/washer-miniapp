CREATE TABLE IF NOT EXISTS franchise_contact (
  id BIGINT NOT NULL AUTO_INCREMENT,
  contact_name VARCHAR(50) NOT NULL,
  contact_phone VARCHAR(30) NOT NULL,
  source VARCHAR(50) DEFAULT 'miniapp',
  remark VARCHAR(500) DEFAULT '',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_franchise_contact_phone (contact_phone),
  KEY idx_franchise_contact_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='加盟联系表';
