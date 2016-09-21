-- Package

ALTER TABLE Package ADD uuid CHAR(36) FIRST ;

UPDATE Package p, (SELECT uuid() uuid, name, version, namespace From Package) ids
SET p.uuid = ids.uuid where p.name = ids.name and p.version = ids.version and p.namespace = ids.namespace
;

alter table Package add CONSTRAINT unique_namespace_name_version UNIQUE(namespace, name, version);

alter table Package drop primary key, add primary key(uuid) ;

ALTER TABLE Package CHANGE uuid uuid CHAR(36) NOT NULL ;


-- InstalledPackage

ALTER TABLE InstalledPackage ADD package_uuid CHAR(36) NOT NULL DEFAULT 0 AFTER device_uuid;

update InstalledPackage ip join Package p on
p.name = ip.packageName and p.version = ip.packageVersion and ip.namespace = p.namespace
set ip.package_uuid = p.uuid;

ALTER TABLE InstalledPackage CHANGE package_uuid package_uuid CHAR(36) NOT NULL;

ALTER TABLE InstalledPackage ADD CONSTRAINT fk_installed_package_uuid
FOREIGN KEY (package_uuid) REFERENCES Package(uuid);

alter table InstalledPackage drop foreign key installed_package_package_fk ;

alter table InstalledPackage drop primary key, add primary key(device_uuid, package_uuid) ;

alter table InstalledPackage drop packageName, drop packageVersion, drop namespace;

-- PackageFilter

ALTER TABLE PackageFilter ADD package_uuid CHAR(36) NOT NULL DEFAULT 0 FIRST;

update PackageFilter pf join Package p on
p.name = pf.packageName and p.version = pf.packageVersion and pf.namespace = p.namespace
set pf.package_uuid = p.uuid;

ALTER TABLE PackageFilter CHANGE package_uuid package_uuid CHAR(36) NOT NULL;

alter table PackageFilter drop foreign key package_fk ;

ALTER TABLE PackageFilter ADD CONSTRAINT fk_package_filter_package_uuid
FOREIGN KEY (package_uuid) REFERENCES Package(uuid);

alter table PackageFilter drop primary key, add primary key(package_uuid, filterName) ;

alter table PackageFilter drop packageName, drop packageVersion;
