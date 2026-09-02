-- Kindergarten 도메인 정적 조회 스키마 (KD3-413)
-- 출처: daeng_v1_back의 크롤링 JSON(info_new.json, price_and_product.json). Redis가 아니라 이 스키마가 신규 source of truth (ADR 0011).
-- FK 제약은 걸지 않는다 (docs/conventions/jpa-entity.md §3) — kindergarten_id는 kindergartens.id 값만 저장.
-- 원장 권한/프로필 편집, 평균가(avg_price_per_time.json), 갤러리 이미지(크롤링 데이터에 없음)는 이번 스코프 밖.

CREATE TABLE kindergartens (
  id                     BIGINT PRIMARY KEY AUTO_INCREMENT,
  naver_place_id         VARCHAR(100),                          -- 크롤링 원본 id. 원장 자체 등록(네이버 미등재) 유치원은 manual_ 접두사 부여, NULL 아님
  name                   VARCHAR(255) NOT NULL,
  phone_number           VARCHAR(255),
  address                VARCHAR(255) NOT NULL,
  road_address           VARCHAR(255),
  latitude               DOUBLE,
  longitude              DOUBLE,
  thumbnail_s3_key       VARCHAR(512),
  visitor_review_count   INT NOT NULL DEFAULT 0,
  blog_review_count      INT NOT NULL DEFAULT 0,
  source                 VARCHAR(20) NOT NULL DEFAULT 'CRAWLED', -- CRAWLED, OWNER_REGISTERED
  status                 VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE, CLOSED. 크롤링 시점엔 항상 ACTIVE — 폐업 갱신은 후속 작업(재크롤링)
  created_at             DATETIME(6) NOT NULL,
  updated_at             DATETIME(6) NOT NULL,
  deleted_at             DATETIME(6),
  UNIQUE (naver_place_id)
);

CREATE TABLE kindergarten_categories (
  id              BIGINT PRIMARY KEY AUTO_INCREMENT,
  kindergarten_id BIGINT NOT NULL,                        -- FK 제약 없음
  category        VARCHAR(30) NOT NULL,                    -- KINDERGARTEN, HOTEL, ...
  created_at      DATETIME(6) NOT NULL,
  updated_at      DATETIME(6) NOT NULL,
  deleted_at      DATETIME(6),
  UNIQUE (kindergarten_id, category)
);
CREATE INDEX idx_kindergarten_categories_kindergarten_id ON kindergarten_categories (kindergarten_id);

CREATE TABLE kindergarten_business_hours (
  id              BIGINT PRIMARY KEY AUTO_INCREMENT,
  kindergarten_id BIGINT NOT NULL,                        -- FK 제약 없음
  name            VARCHAR(30) NOT NULL,                    -- DEFAULT, KINDERGARTEN, HOTEL ... (실제로 프로필별로 나뉘어 관리됨)
  weekday_open    TIME(6),
  weekday_close   TIME(6),
  weekend_open    TIME(6),
  weekend_close   TIME(6),
  offdays         JSON,                                     -- 휴무 요일. 예: ["THURSDAY"]
  created_at      DATETIME(6) NOT NULL,
  updated_at      DATETIME(6) NOT NULL,
  deleted_at      DATETIME(6),
  UNIQUE (kindergarten_id, name)
);
CREATE INDEX idx_kindergarten_business_hours_kindergarten_id ON kindergarten_business_hours (kindergarten_id);

CREATE TABLE kindergarten_links (
  id              BIGINT PRIMARY KEY AUTO_INCREMENT,
  kindergarten_id BIGINT NOT NULL,                        -- FK 제약 없음
  code            VARCHAR(20) NOT NULL,                    -- HOMEPAGE, INSTAGRAM, YOUTUBE, BLOG ...
  url             VARCHAR(2048) NOT NULL,
  created_at      DATETIME(6) NOT NULL,
  updated_at      DATETIME(6) NOT NULL,
  deleted_at      DATETIME(6),
  UNIQUE (kindergarten_id, code)
);
CREATE INDEX idx_kindergarten_links_kindergarten_id ON kindergarten_links (kindergarten_id);

CREATE TABLE kindergarten_options (
  id              BIGINT PRIMARY KEY AUTO_INCREMENT,
  kindergarten_id BIGINT NOT NULL,                        -- FK 제약 없음
  option_group    VARCHAR(30) NOT NULL,                    -- DOG_BREED, DOG_SERVICE, SAFETY_FACILITY, VISITOR_AMENITY
  option_code     VARCHAR(50) NOT NULL,
  created_at      DATETIME(6) NOT NULL,
  updated_at      DATETIME(6) NOT NULL,
  deleted_at      DATETIME(6),
  UNIQUE (kindergarten_id, option_group, option_code)
);
CREATE INDEX idx_kindergarten_options_kindergarten_id ON kindergarten_options (kindergarten_id);

CREATE TABLE kindergarten_price_images (                   -- info_new.json의 menu_image_s3_keys 배열
  id              BIGINT PRIMARY KEY AUTO_INCREMENT,
  kindergarten_id BIGINT NOT NULL,                        -- FK 제약 없음
  s3_key          VARCHAR(512) NOT NULL,
  display_order   INT NOT NULL DEFAULT 0,
  created_at      DATETIME(6) NOT NULL,
  updated_at      DATETIME(6) NOT NULL,
  deleted_at      DATETIME(6),
  UNIQUE (kindergarten_id, s3_key)
);
CREATE INDEX idx_kindergarten_price_images_kindergarten_id ON kindergarten_price_images (kindergarten_id);

CREATE TABLE kindergarten_menus (                           -- price_and_product.json
  id                     BIGINT PRIMARY KEY AUTO_INCREMENT,
  kindergarten_id        BIGINT NOT NULL,                  -- FK 제약 없음
  product_type           VARCHAR(30) NOT NULL,              -- COUNT_TICKET, MONTHLY_TICKET, MEMBERSHIP
  service_type           VARCHAR(30) NOT NULL,              -- DAYCARE, HOTEL ...
  product_name           VARCHAR(200) NOT NULL,
  unit                   DOUBLE,
  unit_str               VARCHAR(50),
  unit_type              VARCHAR(20),
  weight_range           VARCHAR(50),
  price                  INT,
  hourly_price           INT,
  is_min_price           BOOLEAN NOT NULL DEFAULT FALSE,
  is_max_price           BOOLEAN NOT NULL DEFAULT FALSE,
  total_duration_str     VARCHAR(100),
  total_duration_minutes INT,
  display_order          INT NOT NULL DEFAULT 0,
  created_at             DATETIME(6) NOT NULL,
  updated_at             DATETIME(6) NOT NULL,
  deleted_at             DATETIME(6)                        -- 자연키 UNIQUE 없음 — 원장 편집 시 항목 숨김/복구 대비
);
-- 다른 하위 테이블과 달리 자연키 UNIQUE가 없어 kindergarten_id 인덱스가 저절로 생기지 않는다 — 상세 조회마다 풀스캔되는 것을 방지
CREATE INDEX idx_kindergarten_menus_kindergarten_id ON kindergarten_menus (kindergarten_id);
