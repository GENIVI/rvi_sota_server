-- First, clear all foreign key constraints and update primary keys.

ALTER TABLE UpdateRequest
DROP FOREIGN KEY fk_update_request_package_id,
DROP KEY fk_update_request_package_id;

ALTER TABLE UpdateSpec
DROP FOREIGN KEY fk_update_specs_vehicle,
DROP KEY fk_update_specs_vehicle;

ALTER TABLE RequiredPackage
DROP FOREIGN KEY fk_downloads_update_specs,
DROP FOREIGN KEY fk_downloads_package,
DROP KEY fk_downloads_package;

ALTER TABLE InstallHistory
DROP FOREIGN KEY install_history_vin_fk,
DROP KEY install_history_vin_fk,
DROP FOREIGN KEY install_history_package_id_fk,
DROP KEY install_history_package_id_fk;

-- Then, update primary keys.

ALTER TABLE Vehicle
ADD namespace CHAR(200) NOT NULL,
DROP PRIMARY KEY,
ADD PRIMARY KEY (namespace, vin);

ALTER TABLE Package
ADD namespace CHAR(200) NOT NULL,
DROP PRIMARY KEY,
ADD PRIMARY KEY (namespace, name, version);

ALTER TABLE RequiredPackage
ADD namespace CHAR(200) NOT NULL,
DROP PRIMARY KEY,
ADD PRIMARY KEY (namespace, update_request_id, vin, package_name, package_version);

ALTER TABLE InstallHistory
ADD namespace CHAR(200) NOT NULL;

-- At last, re-wire all foreign key constraints

ALTER TABLE UpdateRequest
ADD namespace CHAR(200) NOT NULL,
ADD FOREIGN KEY fk_update_request_package_id (namespace, package_name, package_version) REFERENCES Package(namespace, name, version);

ALTER TABLE UpdateSpec
ADD namespace CHAR(200) NOT NULL,
ADD FOREIGN KEY fk_update_specs_vehicle (namespace, vin) REFERENCES Vehicle(namespace, vin);

ALTER TABLE RequiredPackage
ADD FOREIGN KEY fk_downloads_update_specs (update_request_id, vin) REFERENCES UpdateSpec(update_request_id, vin),
ADD FOREIGN KEY fk_downloads_package (namespace, package_name, package_version) REFERENCES Package(namespace, name, version);

ALTER TABLE InstallHistory
ADD FOREIGN KEY install_history_vin_fk (namespace, vin) REFERENCES Vehicle(namespace, vin),
ADD FOREIGN KEY install_history_package_id_fk (namespace, packageName, packageVersion) REFERENCES Package(namespace, name, version);
