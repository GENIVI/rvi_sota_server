-- Package installed on a VIN.
CREATE TABLE InstalledPackage (
    vin            varchar(64)  NOT NULL,
    packageName    varchar(200) NOT NULL,
    packageVersion varchar(200) NOT NULL,

    PRIMARY KEY(vin, packageName, packageVersion),
    FOREIGN KEY installed_package_vin_fk(vin) REFERENCES Vehicle(vin),
    FOREIGN KEY installed_package_package_fk(packageName, packageVersion) REFERENCES Package(name, version)
);
