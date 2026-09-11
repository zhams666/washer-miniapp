ALTER TABLE store
  ADD COLUMN IF NOT EXISTS cover_image VARCHAR(500) DEFAULT NULL,
  ADD COLUMN IF NOT EXISTS door_close_interval_one_start INTEGER DEFAULT NULL,
  ADD COLUMN IF NOT EXISTS door_close_interval_one_end INTEGER DEFAULT NULL,
  ADD COLUMN IF NOT EXISTS door_close_interval_two_start INTEGER DEFAULT NULL,
  ADD COLUMN IF NOT EXISTS door_close_interval_two_end INTEGER DEFAULT NULL,
  ADD COLUMN IF NOT EXISTS register_reward_amount NUMERIC(12,2) DEFAULT NULL,
  ADD COLUMN IF NOT EXISTS invite_reward_amount NUMERIC(12,2) DEFAULT NULL,
  ADD COLUMN IF NOT EXISTS activity_intro VARCHAR(1000) DEFAULT NULL,
  ADD COLUMN IF NOT EXISTS recharge_description VARCHAR(1000) DEFAULT NULL,
  ADD COLUMN IF NOT EXISTS member_description VARCHAR(1000) DEFAULT NULL,
  ADD COLUMN IF NOT EXISTS cabinet_min_recharge_amount NUMERIC(12,2) DEFAULT NULL,
  ADD COLUMN IF NOT EXISTS cabinet_min_balance_amount NUMERIC(12,2) DEFAULT NULL;

ALTER TABLE ranking_display_adjustment
  ADD COLUMN IF NOT EXISTS scope VARCHAR(20) NOT NULL DEFAULT 'total';

CREATE INDEX IF NOT EXISTS ranking_display_adjustment__idx_scope_occurred_at
  ON ranking_display_adjustment (scope, occurred_at);

CREATE INDEX IF NOT EXISTS ranking_display_adjustment__idx_scope_user_occurred_at
  ON ranking_display_adjustment (scope, user_id, occurred_at);
