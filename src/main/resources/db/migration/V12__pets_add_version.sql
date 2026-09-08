ALTER TABLE pets
  ADD COLUMN version BIGINT NOT NULL DEFAULT 0 AFTER representative_user_id;
