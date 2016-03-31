-- 
-- Copyright: Copyright (C) 2015, Jaguar Land Rover
-- License: MPL-2.0
-- 
CREATE TABLE Firmware (
  module varchar(200) NOT NULL,
  firmware_id varchar(200) NOT NULL,
  last_modified BigInt NOT NULL,
  vin varchar(64) NOT NULL,

  PRIMARY KEY (module, firmware_id, vin),
  FOREIGN KEY fk_firmware_vehicle (vin) REFERENCES Vehicle(vin)
);

ALTER TABLE Package
ADD COLUMN last_modified BigInt
