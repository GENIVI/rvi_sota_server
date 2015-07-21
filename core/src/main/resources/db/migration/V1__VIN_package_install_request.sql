CREATE TABLE Vin (
    vin varchar(64) NOT NULL, -- SRV-VIN-2 A VIN shall be identified by an 1-64 byte string

    PRIMARY KEY (vin)
);

CREATE TABLE Package (
    -- Surrogate key
    id BIGINT NOT NULL AUTO_INCREMENT,
    -- Natural key is name + version
    name varchar(200) NOT NULL,
    version varchar(200) NOT NULL,
    -- Free form description
    description text,
    -- Free form information about the vendor
    vendor text,

    PRIMARY KEY (id),
    UNIQUE KEY name_version_unique (name, version)
);

CREATE TABLE InstallRequest (
	id BIGINT NOT NULL AUTO_INCREMENT,
	packageId BIGINT NOT NULL,
	priority INT NOT NULL,
	startAfter datetime NOT NULL,
	endBefore datetime NOT NULL,

    PRIMARY KEY (id),
    FOREIGN KEY package_id_fk (packageId) REFERENCES Package(id)
);