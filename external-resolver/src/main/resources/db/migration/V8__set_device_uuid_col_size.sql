ALTER TABLE Firmware MODIFY device_uuid char(36);

ALTER TABLE InstalledPackage MODIFY device_uuid char(36);

ALTER TABLE InstalledComponent MODIFY device_uuid char(36);

