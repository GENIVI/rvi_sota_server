CREATE TABLE Filter (
    -- Natural key
    name varchar(200) NOT NULL,
    -- Filter expression
    expression text NOT NULL,

    PRIMARY KEY (name),
    UNIQUE KEY name_unique (name)
);

-- This filter is relevant to this package, and vice versa
-- Note that packages are identified by name and not name+version, so filters
-- remain relevant when packages are upgraded
CREATE TABLE RelatedPackages (
    filterId BIGINT NOT NULL,
    packageName varchar(200) NOT NULL,

    PRIMARY KEY(FilterId, PackageName)
);
