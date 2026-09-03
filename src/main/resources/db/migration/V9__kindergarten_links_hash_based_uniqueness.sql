ALTER TABLE kindergarten_links DROP INDEX uk_kindergarten_links_kindergarten_id_code_url;

ALTER TABLE kindergarten_links ADD COLUMN url_hash CHAR(64) AS (SHA2(url, 256)) STORED;

ALTER TABLE kindergarten_links ADD UNIQUE KEY uk_kindergarten_links_kindergarten_id_code_url_hash (kindergarten_id, code, url_hash);
