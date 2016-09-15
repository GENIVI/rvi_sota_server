create table `Campaign` (
`uuid` char(36) NOT NULL,
`namespace` char(200) NOT NULL,
`name` varchar(200) NOT NULL,
`launched` boolean NOT NULL,
`package_uuid` char(36) NULL,

 PRIMARY KEY (uuid),

 CONSTRAINT `Campaign_pkg_fk`
 FOREIGN KEY (package_uuid)
 REFERENCES `Package` (uuid)
);

create unique index `Campaign_unique_name` on
`Campaign` (`namespace`, `name`)
;

create table `CampaignGroups` (
`campaign_id` char(36) NOT NULL,
`group_name` varchar(200) NOT NULL,

 PRIMARY KEY (campaign_id, group_name)
);
