CREATE TABLE `Image` (
  `namespace` char(200) NOT NULL,
  `uuid` char(36) NOT NULL,
  `commit` char(64) NOT NULL,
  `ref` varchar(255) NOT NULL,
  `description` mediumtext NOT NULL,
  `pull_url` TEXT NOT NULL,
  `created_at` DATETIME(3) NOT NULL,
  `updated_at` DATETIME(3) NOT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `image_unique_namespace_commit` (`namespace`,`ref`)
)
;

create table `ImageUpdate`(
`namespace` VARCHAR(200) NOT NULL,
`uuid` char(36) NOT NULL,
`image_uuid` char(36) NOT NULL,
`device_uuid` char(36) NOT NULL,
`status` varchar(200) NOT NULL,
`created_at` DATETIME(3) NOT NULL,
`updated_at` DATETIME(3) NOT NULL,

UNIQUE KEY `image_update_unique_uuid_device_uuid` (`image_uuid`,`device_uuid`),
PRIMARY KEY (`uuid`),
CONSTRAINT `fk_image_update_image` foreign key(`image_uuid`) references `Image`(`uuid`)
)
;
