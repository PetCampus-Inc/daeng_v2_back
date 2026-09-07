ALTER TABLE kindergartens DROP COLUMN address;
ALTER TABLE kindergartens CHANGE COLUMN road_address address VARCHAR(255) NOT NULL;
ALTER TABLE kindergartens ADD COLUMN address_detail VARCHAR(100) AFTER address;
