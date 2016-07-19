alter table Firmware drop foreign key fk_firmware_vehicle ;

alter table InstalledComponent drop foreign key installed_component_vin_fk ;

alter table InstalledPackage drop foreign key installed_package_vin_fk ;

DROP table Vehicle ;

ALTER TABLE InstalledPackage CHANGE vin device_uuid char(64);

ALTER TABLE InstalledComponent CHANGE vin device_uuid char(64);

ALTER TABLE Firmware CHANGE vin device_uuid char(64);
