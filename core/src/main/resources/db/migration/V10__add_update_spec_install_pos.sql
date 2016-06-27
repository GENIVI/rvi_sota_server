ALTER TABLE UpdateRequest
DROP COLUMN install_pos
;

ALTER TABLE UpdateSpec
ADD install_pos int NOT NULL
;
