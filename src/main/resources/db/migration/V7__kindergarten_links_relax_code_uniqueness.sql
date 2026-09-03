SET @idx := (
  SELECT INDEX_NAME FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'kindergarten_links'
    AND NON_UNIQUE = 0
    AND COLUMN_NAME = 'kindergarten_id'
  LIMIT 1
);
SET @sql := CONCAT('ALTER TABLE kindergarten_links DROP INDEX ', @idx);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

ALTER TABLE kindergarten_links ADD UNIQUE KEY uk_kindergarten_links_kindergarten_id_code_url (kindergarten_id, code, url(255));
