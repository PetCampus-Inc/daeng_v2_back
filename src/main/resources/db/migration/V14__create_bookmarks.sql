CREATE TABLE bookmarks (
  id              BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_code       VARCHAR(8)   NOT NULL,
  kindergarten_id VARCHAR(100) NOT NULL,
  created_at      DATETIME(6)  NOT NULL,
  updated_at      DATETIME(6)  NOT NULL,
  deleted_at      DATETIME(6),
  UNIQUE (user_code, kindergarten_id)
);
CREATE INDEX idx_bookmarks_user_code_created_at ON bookmarks (user_code, created_at);
