CREATE TABLE users (
  id                      BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_code               VARCHAR(8)   NOT NULL,
  nickname                VARCHAR(100),
  profile_image           VARCHAR(500),
  info_receive_email      VARCHAR(255),
  gender                  VARCHAR(20),
  phone_number            VARCHAR(20),
  emergency_phone_number  VARCHAR(20),
  created_at              DATETIME(6) NOT NULL,
  updated_at              DATETIME(6) NOT NULL,
  deleted_at              DATETIME(6),
  UNIQUE (user_code)
);

-- provider별 동일 이메일 다중 계정을 허용해야 하므로(레거시 VerifyOidcService 로직 포팅), email에는 UNIQUE 제약을 걸지 않는다.
CREATE TABLE social_users (
  id           BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id      BIGINT,                        -- FK 제약 없음 (docs/conventions/jpa-entity.md)
  provider     VARCHAR(20)  NOT NULL,
  provider_id  VARCHAR(255) NOT NULL,
  email        VARCHAR(255) NOT NULL,
  name         VARCHAR(255),
  picture      VARCHAR(500),
  status       VARCHAR(20)  NOT NULL,
  linked_at    DATETIME(6),
  created_at   DATETIME(6) NOT NULL,
  updated_at   DATETIME(6) NOT NULL,
  deleted_at   DATETIME(6),
  UNIQUE (provider, provider_id)
);

CREATE TABLE user_addresses (
  id            BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id       BIGINT NOT NULL,               -- FK 제약 없음
  type          VARCHAR(20) NOT NULL,
  alias         VARCHAR(20),
  address       VARCHAR(200) NOT NULL,
  road_address  VARCHAR(200),
  address_detail VARCHAR(100),                 -- v0 계약: 프론트가 화면에 표시한다
  lat           DOUBLE NOT NULL,
  lng           DOUBLE NOT NULL,
  created_at    DATETIME(6) NOT NULL,
  updated_at    DATETIME(6) NOT NULL,
  deleted_at    DATETIME(6)
);
