-- First, clear all foreign key constraints and update primary keys.
ALTER TABLE InstalledPackage
DROP FOREIGN KEY installed_package_vin_fk;

ALTER TABLE InstalledComponent
DROP FOREIGN KEY installed_component_vin_fk;

ALTER TABLE PackageFilter
DROP FOREIGN KEY package_fk,
DROP FOREIGN KEY filter_fk,
DROP KEY filter_fk;

ALTER TABLE InstalledPackage
DROP FOREIGN KEY installed_package_package_fk,
DROP KEY installed_package_package_fk;

ALTER TABLE InstalledComponent
DROP FOREIGN KEY installed_component_partNumber_fk,
DROP KEY installed_component_partNumber_fk;

ALTER TABLE Firmware
DROP FOREIGN KEY fk_firmware_vehicle,
DROP KEY fk_firmware_vehicle;

-- Then, update primary keys.

ALTER TABLE Vehicle
ADD namespace CHAR(200) NOT NULL,
DROP PRIMARY KEY,
ADD PRIMARY KEY (namespace, vin);

ALTER TABLE Package
ADD namespace CHAR(200) NOT NULL,
DROP PRIMARY KEY,
ADD PRIMARY KEY (namespace, name, version);

ALTER TABLE Filter
ADD namespace CHAR(200) NOT NULL,
DROP KEY name_unique,
ADD UNIQUE KEY name_unique (namespace, name),
DROP PRIMARY KEY,
ADD PRIMARY KEY (namespace, name);

ALTER TABLE PackageFilter
ADD namespace CHAR(200) NOT NULL,
DROP PRIMARY KEY,
ADD PRIMARY KEY (namespace, packageName, packageVersion, filterName);

ALTER TABLE InstalledPackage
ADD namespace CHAR(200) NOT NULL,
DROP PRIMARY KEY,
ADD PRIMARY KEY (namespace, vin, packageName, packageVersion);

ALTER TABLE InstalledComponent
ADD namespace CHAR(200) NOT NULL,
DROP PRIMARY KEY,
ADD PRIMARY KEY (namespace, vin, partNumber);

ALTER TABLE Component
ADD namespace CHAR(200) NOT NULL,
DROP PRIMARY KEY,
ADD PRIMARY KEY (namespace, partnumber);

ALTER TABLE Firmware
ADD namespace CHAR(200) NOT NULL,
DROP PRIMARY KEY,
ADD PRIMARY KEY (namespace, module, firmware_id, vin);

-- At last, re-wire all foreign key constraints

ALTER TABLE InstalledPackage
ADD FOREIGN KEY installed_package_vin_fk (namespace, vin) REFERENCES Vehicle(namespace, vin);

ALTER TABLE InstalledComponent
ADD FOREIGN KEY installed_component_vin_fk (namespace, vin) REFERENCES Vehicle(namespace, vin);

ALTER TABLE PackageFilter
ADD FOREIGN KEY package_fk (namespace, packageName, packageVersion) REFERENCES Package(namespace, name, version),
ADD FOREIGN KEY filter_fk (namespace, filterName) REFERENCES Filter(namespace, name);

ALTER TABLE InstalledPackage
ADD FOREIGN KEY installed_package_package_fk (namespace, packageName, packageVersion) REFERENCES Package(namespace, name, version);

ALTER TABLE InstalledComponent
ADD FOREIGN KEY installed_component_partNumber_fk (namespace, partNumber) REFERENCES Component(namespace, partNumber);

ALTER TABLE Firmware
ADD FOREIGN KEY fk_firmware_vehicle (namespace, vin) REFERENCES Vehicle(namespace, vin);
