
ALTER TABLE DeviceGroup ADD id char(36) NULL;

update DeviceGroup set id = (select uuid()) ;

alter table DeviceGroup drop primary key, add primary key(id) ;

ALTER TABLE DeviceGroup CHANGE id id CHAR(36) NOT NULL FIRST ;

ALTER TABLE DeviceGroup
ADD CONSTRAINT UNIQUE (namespace, group_name);

ALTER TABLE GroupMembers ADD group_id char(36) NULL;

update GroupMembers gm, DeviceGroup dg set gm.group_id = dg.id
where gm.group_name = dg.group_name and gm.namespace = dg.namespace
;

ALTER TABLE GroupMembers CHANGE group_id group_id char(36) NOT NULL;

ALTER TABLE GroupMembers ADD FOREIGN KEY (group_id) REFERENCES DeviceGroup(id) ;

ALTER TABLE GroupMembers
DROP PRIMARY KEY,
DROP COLUMN group_name,
ADD PRIMARY KEY (group_id, device_uuid);
