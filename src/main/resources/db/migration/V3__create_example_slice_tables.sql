-- 예제 슬라이스(owner, bookmark) 테이블. KD3-197에서 ddl-auto:update로 만들어졌던 것을
-- Flyway로 옮긴다. KD3-258이 JPA_DDL_AUTO=validate로 전환하면서 이 테이블들이
-- 마이그레이션에 없으면 빈 DB에서 앱이 뜨지 못한다(Schema-validation: missing table).
--
-- IF NOT EXISTS인 이유: 기존 로컬 DB에는 ddl-auto:update가 만든 두 테이블이 이미 있고,
-- application-local.yaml이 baseline-on-migrate로 그 DB를 그대로 인정하기 때문에
-- 같은 마이그레이션이 "테이블이 있는 DB"와 "빈 DB" 양쪽에서 모두 돌아야 한다.
CREATE TABLE IF NOT EXISTS owner (
  id      VARCHAR(36)  NOT NULL PRIMARY KEY,
  email   VARCHAR(255) NOT NULL,
  status  VARCHAR(20)  NOT NULL,
  UNIQUE (email)
);

CREATE TABLE IF NOT EXISTS bookmark (
  id               VARCHAR(36) NOT NULL PRIMARY KEY,
  owner_id         VARCHAR(36) NOT NULL,
  kindergarten_id  VARCHAR(36) NOT NULL
);
