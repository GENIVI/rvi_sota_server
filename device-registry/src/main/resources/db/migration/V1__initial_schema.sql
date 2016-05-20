ALTER DATABASE CHARACTER SET utf8;
ALTER DATABASE COLLATE utf8_bin;

CREATE TABLE Device (
  namespace CHAR(200) NOT NULL,
  uuid char(36) NOT NULL,
  device_id varchar(200) UNIQUE,
  device_type SMALLINT NOT NULL,
  last_seen DATETIME NULL,

  PRIMARY KEY (uuid)
);

CREATE TABLE DeviceType (
  id SMALLINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(200) NULL
);

-- populate device types

INSERT INTO DeviceType (name) VALUES ("Other");
INSERT INTO DeviceType (name) VALUES ("Vehicle");
