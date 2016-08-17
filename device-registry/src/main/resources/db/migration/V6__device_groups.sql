
CREATE TABLE DeviceGroup (
  group_name VARCHAR(200) NOT NULL,
  namespace CHAR(200) NOT NULL,
  group_info LONGTEXT NOT NULL,

  PRIMARY KEY (group_name, namespace)
);
