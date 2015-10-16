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
