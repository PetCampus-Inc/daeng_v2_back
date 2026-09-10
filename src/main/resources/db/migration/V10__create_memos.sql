-- 자유메모 (KD3-465). 레거시 free_memo를 재설계 — 매 저장 새 row + 히스토리를 버리고
-- (user_code, target_id) 1행 upsert. user_code는 auth 토큰 subject(UserCode),
-- target_id는 kindergartens.naver_place_id. FK 제약 없음 (docs/conventions/jpa-entity.md).
CREATE TABLE memos (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_code   VARCHAR(8)   NOT NULL,
  target_id   VARCHAR(100) NOT NULL,
  content     TEXT,
  created_at  DATETIME(6)  NOT NULL,
  updated_at  DATETIME(6)  NOT NULL,
  deleted_at  DATETIME(6),
  UNIQUE (user_code, target_id)
);
CREATE INDEX idx_memos_user_code_updated_at ON memos (user_code, updated_at);
