CREATE TABLE InstalledPackage (
    device_uuid char(64) NOT NULL,
    name varchar(200) NOT NULL,
    version varchar(200) NOT NULL,
    last_modified DATETIME NOT NULL,

    PRIMARY KEY(device_uuid, name, version)
);
