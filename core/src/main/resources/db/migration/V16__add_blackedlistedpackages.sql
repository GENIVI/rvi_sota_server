create table `BlacklistedPackage` (
`uuid` char(36) NOT NULL,
`namespace` char(200) NOT NULL,
`package_name` varchar(200) NOT NULL,
`package_version` varchar(200) NOT NULL,
`active` boolean NOT NULL default true,
`comment` TEXT NOT NULL,
`updated_at` TIMESTAMP NOT NULL,

 PRIMARY KEY (uuid)
);

alter table `BlacklistedPackage`
add constraint `BlacklistedPackage_pkg_fk`
foreign key (`namespace`,`package_name`,`package_version`)
references `Package`(`namespace`,`name`,`version`)
;
