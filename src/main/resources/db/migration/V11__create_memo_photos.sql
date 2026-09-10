-- 자유메모 첨부 사진 (KD3-465). 텍스트 메모(memos)와 독립된 기능 — memos row에 매이지 않고
-- (user_code, target_id)를 직접 키로 갖는다. 개별 추가/삭제, 유치원당 최대 5장(애플리케이션 검증).
-- object_key는 media commit 후 영구 key. BaseEntity 미상속(라이프사이클을 도메인이 관리).
CREATE TABLE memo_photos (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_code   VARCHAR(8)   NOT NULL,
  target_id   VARCHAR(100) NOT NULL,
  object_key  VARCHAR(512) NOT NULL,
  sort_order  INT          NOT NULL,
  created_at  DATETIME(6)  NOT NULL,
  UNIQUE (user_code, target_id, object_key)
);
CREATE INDEX idx_memo_photos_user_code_target_id ON memo_photos (user_code, target_id);
