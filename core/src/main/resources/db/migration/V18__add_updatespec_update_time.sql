ALTER TABLE UpdateSpec
ADD update_time DATETIME(3) NOT NULL DEFAULT 0
;

UPDATE UpdateSpec set update_time = creation_time ;

ALTER TABLE UpdateSpec
MODIFY update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP
;
