ALTER TABLE `Campaign`
ADD COLUMN `status` ENUM('Draft', 'InPreparation', 'Active') NOT NULL DEFAULT 'Draft',
ADD COLUMN `delta_from_name` varchar(200),
ADD COLUMN `delta_from_version` varchar(200),
ADD COLUMN `size` Long;

UPDATE `Campaign`
SET `status` = 'Active'
WHERE `launched` = 1;

ALTER TABLE `Campaign`
DROP COLUMN `launched`;

CREATE TABLE `LaunchCampaignRequests` (
  `campaign_id` char(36) NOT NULL PRIMARY KEY,
  `start_date` DATETIME(3),
  `end_date` DATETIME(3),
  `priority` int,
  `signature` char(254),
  `description` char(254),
  `request_confirmation` boolean,


 CONSTRAINT `LaunchCampaignRequests_id_fk`
 FOREIGN KEY (campaign_id)
 REFERENCES `Campaign` (uuid)
);

