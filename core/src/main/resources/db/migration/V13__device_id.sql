-- First, clear all foreign key constraints

ALTER TABLE UpdateSpec
DROP FOREIGN KEY fk_update_specs_vehicle;

ALTER TABLE RequiredPackage
DROP FOREIGN KEY fk_downloads_update_specs;

ALTER TABLE InstallHistory
DROP FOREIGN KEY install_history_vin_fk;

ALTER TABLE OperationResult
DROP FOREIGN KEY fk_operation_result_vin;

-- Then drop vehicles table

DROP TABLE Vehicle;

-- At last, re-wire all foreign key constraints

ALTER TABLE UpdateSpec
CHANGE vin device_uuid CHAR(36) NOT NULL;

ALTER TABLE RequiredPackage
CHANGE vin device_uuid CHAR(36) NOT NULL;

ALTER TABLE InstallHistory
CHANGE vin device_uuid CHAR(36) NOT NULL;

ALTER TABLE OperationResult
CHANGE vin device_uuid CHAR(36) NOT NULL;
