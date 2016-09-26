CREATE TABLE GroupMembers (
  group_name VARCHAR(200) NOT NULL,
  namespace CHAR(200) NOT NULL,
  device_uuid char(36) NOT NULL REFERENCES Device(uuid),

  PRIMARY KEY (group_name, namespace, device_uuid)
);
