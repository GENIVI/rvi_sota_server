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
    UNIQUE KEY campaign_package_vin_unique (installCampaignId,
    packageId, vin),
    FOREIGN KEY install_campaign_id_fk (installCampaignId) REFERENCES
    InstallCampaign(id),
    FOREIGN KEY install_request_package_id_fk (packageId) REFERENCES Package(id),
    FOREIGN KEY install_request_vin_fk (vin) REFERENCES Vin(vin)
);
