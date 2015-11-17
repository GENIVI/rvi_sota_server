CREATE TABLE Vehicle (
    vin varchar(64) NOT NULL, -- SRV-VIN-2 A VIN shall be identified by an 1-64 byte string

    PRIMARY KEY (vin)
);

CREATE TABLE Package (
    -- Natural key is name + version
    name varchar(200) NOT NULL,
    version varchar(200) NOT NULL,
    -- Free form description
    description text,
    -- Free form information about the vendor
    vendor text,

    PRIMARY KEY (name, version)
);

CREATE TABLE Filter (
    -- Natural key
    name varchar(200) NOT NULL,
    -- Filter expression
    expression text NOT NULL,

    PRIMARY KEY (name),
    UNIQUE KEY name_unique (name)
);

-- This filter is relevant to this package, and vice versa.
CREATE TABLE PackageFilter (
    packageName    varchar(200) NOT NULL,
    packageVersion varchar(200) NOT NULL,
    filterName     varchar(200) NOT NULL,

    PRIMARY KEY(packageName, packageVersion, filterName),
    FOREIGN KEY package_fk(packageName, packageVersion) REFERENCES Package(name, version),
    FOREIGN KEY filter_fk(filterName) REFERENCES Filter(name)
);

-- Package installed on a VIN.
CREATE TABLE InstalledPackage (
    vin            varchar(64)  NOT NULL,
    packageName    varchar(200) NOT NULL,
    packageVersion varchar(200) NOT NULL,

    PRIMARY KEY(vin, packageName, packageVersion),
    FOREIGN KEY installed_package_vin_fk(vin) REFERENCES Vehicle(vin),
    FOREIGN KEY installed_package_package_fk(packageName, packageVersion) REFERENCES Package(name, version)
);

CREATE TABLE Component (
    partNumber     varchar(200) NOT NULL,
    description    text,

    PRIMARY KEY(partnumber)
);

-- Components installed on a VIN.
CREATE TABLE InstalledComponent (
    vin            varchar(64)  NOT NULL,
    partNumber     varchar(200) NOT NULL,

    PRIMARY KEY(vin, partNumber),
    FOREIGN KEY installed_component_vin_fk(vin)               REFERENCES Vehicle(vin),
    FOREIGN KEY installed_component_partNumber_fk(partNumber) REFERENCES Component(partNumber)
);
