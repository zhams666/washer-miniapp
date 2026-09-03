-- Run once in the CloudBase PostgreSQL SQL editor after the earlier initialization scripts.
-- Safe to run repeatedly. It repairs user_info schemas created by earlier revisions.

ALTER TABLE user_info
  ADD COLUMN IF NOT EXISTS points INTEGER NOT NULL DEFAULT 0;

ALTER TABLE user_info
  ADD COLUMN IF NOT EXISTS member_expire_time TIMESTAMP DEFAULT NULL;

