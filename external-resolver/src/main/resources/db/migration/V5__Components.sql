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
