UPDATE DeviceGroup
SET discarded_attrs="null"
WHERE discarded_attrs IS NULL;

ALTER TABLE DeviceGroup
change discarded_attrs discarded_attrs LONGTEXT NOT NULL;
ALTER TABLE DeviceGroup
ALTER discarded_attrs SET DEFAULT "null";