CREATE TABLE `DevicePublicCredentials` (
    `device_uuid` char(36) NOT NULL,
    `public_credentials` LONGBLOB NOT NULL,

    PRIMARY KEY (`device_uuid`)
);
