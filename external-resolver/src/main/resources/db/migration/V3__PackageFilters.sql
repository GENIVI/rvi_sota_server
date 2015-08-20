DROP TABLE RelatedPackages;

-- This filter is relevant to this package, and vice versa.
CREATE TABLE PackageFilters (
    packageName    varchar(200) NOT NULL,
    packageVersion varchar(200) NOT NULL,
    filterName     varchar(200) NOT NULL,

    PRIMARY KEY(packageName, packageVersion, filterName),
    FOREIGN KEY package_fk(packageName, packageVersion) REFERENCES Package(name, version),
    FOREIGN KEY filter_fk(filterName) REFERENCES Filter(name)
);
