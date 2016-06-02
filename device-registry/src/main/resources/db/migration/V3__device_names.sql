ALTER TABLE Device
ADD device_name VARCHAR(200) NOT NULL,
DROP INDEX device_id,
-- TODO do these cause performance issues?
ADD CONSTRAINT UNIQUE (namespace, device_name),
ADD CONSTRAINT UNIQUE (namespace, device_id);

-- fix device types

DELETE FROM DeviceType ORDER BY id LIMIT 2;

ALTER TABLE DeviceType
DROP PRIMARY KEY,
MODIFY id SMALLINT NOT NULL PRIMARY KEY;

-- populate device types

INSERT INTO DeviceType (id, name) VALUES (0, "Other");
INSERT INTO DeviceType (id, name) VALUES (1, "Vehicle");
