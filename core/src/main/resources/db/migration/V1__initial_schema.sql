CREATE TABLE Vehicle (
    vin varchar(64) NOT NULL, -- SRV-VIN-2 A VIN shall be identified by an 1-64 byte string

    PRIMARY KEY (vin)
);

CREATE TABLE Package (
    -- Natural key is name + version
    name varchar(200) NOT NULL,
    version varchar(200) NOT NULL,
    uri varchar(4096) NOT NULL,
    file_size int NOT NULL,
    check_sum varchar(4096) NOT NULL,
    -- Free form description
    description text,
    -- Free form information about the vendor
    vendor text,

    PRIMARY KEY (name, version)
);

CREATE TABLE UpdateRequest (
    update_request_id CHAR(36) NOT NULL,
    package_name VARCHAR(200) NOT NULL,
    package_version VARCHAR(200) NOT NULL,
    creation_time DATETIME NOT NULL,
    start_after DATETIME NOT NULL,
    finish_before DATETIME NOT NULL,
    priority INT NOT NULL,

    PRIMARY KEY (update_request_id),
    FOREIGN KEY fk_update_request_package_id (package_name, package_version) REFERENCES Package(name, version)
);

CREATE TABLE UpdateSpec (
    update_request_id CHAR(36) NOT NULL,
    vin varchar(64) NOT NULL,
    status VARCHAR(200),

    PRIMARY KEY (update_request_id, vin),
    FOREIGN KEY fk_update_specs_request (update_request_id) REFERENCES UpdateRequest(update_request_id),
    FOREIGN KEY fk_update_specs_vehicle (vin) REFERENCES Vehicle(vin)
);

CREATE TABLE RequiredPackage (
  update_request_id CHAR(36) NOT NULL,
  vin varchar(64) NOT NULL,
  package_name VARCHAR(200) NOT NULL,
  package_version VARCHAR(200) NOT NULL,

  PRIMARY KEY (update_request_id, vin, package_name, package_version),
  FOREIGN KEY fk_downloads_update_specs (update_request_id, vin) REFERENCES UpdateSpec(update_request_id, vin),
  FOREIGN KEY fk_downloads_package (package_name, package_version) REFERENCES Package(name, version)
);

CREATE TABLE InstallHistory (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    vin            VARCHAR(64)  NOT NULL,
    packageName    VARCHAR(200) NOT NULL,
    packageVersion VARCHAR(200) NOT NULL,
    completionTime DATETIME     NOT NULL,
    success        BOOLEAN,

    PRIMARY KEY (id),
    FOREIGN KEY install_history_vin_fk        (vin)                         REFERENCES Vehicle(vin),
    FOREIGN KEY install_history_package_id_fk (packageName, packageVersion) REFERENCES Package(name, version)
);
