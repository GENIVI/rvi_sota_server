ALTER TABLE `Campaign`
ADD COLUMN created_at DATETIME(3) NOT NULL DEFAULT NOW();

ALTER TABLE `Package`
ADD COLUMN created_at_temp DATETIME(3) NOT NULL DEFAULT NOW();

UPDATE `Package`
SET created_at_temp = createdAt;

ALTER TABLE `Package`
DROP COLUMN createdAt;
ALTER TABLE `Package`
CHANGE created_at_temp created_at DATETIME(3) NOT NULL DEFAULT NOW();
