-- Package

ALTER TABLE Package ADD uuid CHAR(36) ;

UPDATE Package p, (SELECT uuid() uuid, name, version, namespace From Package) ids
SET p.uuid = ids.uuid where p.name = ids.name and p.version = ids.version and p.namespace = ids.namespace
;

alter table Package add CONSTRAINT unique_namespace_name_version UNIQUE(namespace, name, version);

alter table Package drop primary key, add primary key(uuid) ;

ALTER TABLE Package CHANGE uuid uuid CHAR(36) NOT NULL ;

-- install history

ALTER TABLE InstallHistory ADD package_uuid CHAR(36) NOT NULL DEFAULT 0;

update InstallHistory ih join Package p on
p.name = ih.packageName and p.version = ih.packageVersion and ih.namespace = p.namespace
set ih.package_uuid = p.uuid;

ALTER TABLE InstallHistory CHANGE package_uuid package_uuid CHAR(36) NOT NULL;

ALTER TABLE InstallHistory ADD CONSTRAINT fk_install_history_package_uuid
FOREIGN KEY (package_uuid) REFERENCES Package(uuid);

alter table InstallHistory drop foreign key install_history_package_id_fk ;

alter table InstallHistory drop packageName, drop packageVersion, drop namespace;

-- required package

ALTER TABLE RequiredPackage ADD package_uuid CHAR(36) NOT NULL DEFAULT 0;

update RequiredPackage rp join Package p on
p.name = rp.package_name and p.version = rp.package_version and rp.namespace = p.namespace
set rp.package_uuid = p.uuid;

ALTER TABLE RequiredPackage CHANGE package_uuid package_uuid CHAR(36) NOT NULL;

alter table RequiredPackage drop primary key, add primary key(package_uuid, update_request_id, device_uuid) ;

ALTER TABLE RequiredPackage ADD CONSTRAINT fk_required_pkg_uuid
FOREIGN KEY (package_uuid) REFERENCES Package(uuid);

alter table RequiredPackage drop foreign key fk_downloads_package ;

alter table RequiredPackage drop package_name, drop package_version, drop namespace;

-- Update Request

ALTER TABLE UpdateRequest ADD package_uuid CHAR(36) NOT NULL DEFAULT 0;

update UpdateRequest ur join Package p on
p.name = ur.package_name and p.version = ur.package_version and ur.namespace = p.namespace
set ur.package_uuid = p.uuid;

ALTER TABLE UpdateRequest CHANGE package_uuid package_uuid CHAR(36) NOT NULL;

ALTER TABLE UpdateRequest ADD CONSTRAINT fk_update_request_pkg_uuid
FOREIGN KEY (package_uuid) REFERENCES Package(uuid);

alter table UpdateRequest drop foreign key  fk_update_request_package_id ;

alter table UpdateRequest drop package_name, drop package_version, drop namespace ;

-- BlacklistedPackage

alter table BlacklistedPackage drop foreign key  BlacklistedPackage_pkg_fk ;

-- update specs

alter table UpdateSpec drop namespace ;

-- Operation Result

alter table OperationResult drop namespace ;

