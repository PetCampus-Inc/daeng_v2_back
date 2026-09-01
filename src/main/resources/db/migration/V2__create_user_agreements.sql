-- 가입 약관 동의 이력 (KD3-258 A-3.5). 레거시 user_agreement(KD3-311)와 같은 개념이다.
-- (user_id, term_type) unique로 재제출 시 중복 행이 쌓이지 않게 하고, 최초 동의 시각을 보존한다.
-- user_id에 FK 제약을 걸지 않는 이유는 docs/conventions/jpa-entity.md 참고.
CREATE TABLE user_agreements (
  id         BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id    BIGINT      NOT NULL,
  term_type  VARCHAR(30) NOT NULL,
  agreed_at  DATETIME(6) NOT NULL,
  UNIQUE (user_id, term_type)
);
