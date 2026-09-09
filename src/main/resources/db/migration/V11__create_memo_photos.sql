-- 자유메모 첨부 사진 (KD3-465). memo 저장 시 photoKeys 배열을 전량 교체(하드 삭제 후 삽입)하므로
-- BaseEntity(soft-delete)를 쓰지 않는다 — user_agreements와 같은 예외. object_key는 media commit 후 영구 key.
CREATE TABLE memo_photos (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  memo_id     BIGINT       NOT NULL,
  object_key  VARCHAR(512) NOT NULL,
  sort_order  INT          NOT NULL,
  created_at  DATETIME(6)  NOT NULL,
  UNIQUE (memo_id, object_key)
);
CREATE INDEX idx_memo_photos_memo_id ON memo_photos (memo_id);
