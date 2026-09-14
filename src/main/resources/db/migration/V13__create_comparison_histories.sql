-- 유치원 비교 히스토리 (KD3-496). 레거시 comparison_history(userId String + kindergartenIds CSV)를
-- user_code + 정렬된 두 유치원 ID로 재설계한다. 두 ID를 사전순 정렬해 저장하고
-- (user_code, kindergarten_id_a, kindergarten_id_b) unique로 [A,B]/[B,A]를 같은 이력으로 dedup한다.
-- compared_at은 별도 컬럼 없이 updated_at을 쓴다. FK 제약 없음 (docs/conventions/jpa-entity.md).
CREATE TABLE comparison_histories (
  id                 BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_code          VARCHAR(8)   NOT NULL,
  kindergarten_id_a  VARCHAR(100) NOT NULL,
  kindergarten_id_b  VARCHAR(100) NOT NULL,
  created_at         DATETIME(6)  NOT NULL,
  updated_at         DATETIME(6)  NOT NULL,
  deleted_at         DATETIME(6),
  UNIQUE (user_code, kindergarten_id_a, kindergarten_id_b)
);
CREATE INDEX idx_comparison_histories_user_code_updated_at ON comparison_histories (user_code, updated_at);
