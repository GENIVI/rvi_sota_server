CREATE TABLE Vehicle (
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

CREATE TABLE InstallCampaign (
    id BIGINT NOT NULL AUTO_INCREMENT,
    -- Head package
    packageId BIGINT NOT NULL,
    priority INT NOT NULL,
    startAfter DATETIME NOT NULL,
    endBefore DATETIME NOT NULL,

    PRIMARY KEY (id),
    FOREIGN KEY install_campaign_package_id_fk (packageId) REFERENCES Package(id)
);

DROP TABLE IF EXISTS InstallRequest;

CREATE TABLE InstallRequest (
    id BIGINT NOT NULL AUTO_INCREMENT,
    installCampaignId BIGINT NOT NULL,
    -- The package to actually install
    packageId BIGINT NOT NULL,
    vin VARCHAR(64) NOT NULL,
    statusCode INT NOT NULL,
    errorMessage TEXT,

    PRIMARY KEY (id),
    UNIQUE KEY campaign_package_vin_unique (installCampaignId, packageId, vin),
    FOREIGN KEY install_campaign_id_fk (installCampaignId) REFERENCES InstallCampaign(id),
    FOREIGN KEY install_request_package_id_fk (packageId) REFERENCES Package(id),
    FOREIGN KEY install_request_vin_fk (vin) REFERENCES Vehicle(vin)
);