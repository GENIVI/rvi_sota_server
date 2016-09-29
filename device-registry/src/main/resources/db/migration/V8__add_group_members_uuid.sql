ALTER TABLE DeviceGroup
DROP PRIMARY KEY,
ADD id char(36) NOT NULL PRIMARY KEY,
ADD CONSTRAINT UNIQUE (namespace, group_name);

ALTER TABLE GroupMembers
DROP PRIMARY KEY,
ADD group_id char(36) NOT NULL REFERENCES DeviceGroup(id),
DROP COLUMN group_name,
ADD PRIMARY KEY (group_id, device_uuid);
