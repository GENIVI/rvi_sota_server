ALTER TABLE `CampaignGroups`
DROP COLUMN `group_name`,
DROP PRIMARY KEY,
ADD `group_uuid` CHAR(36) NOT NULL,
ADD PRIMARY KEY (campaign_id, group_uuid),
ADD `update_request_id` CHAR(36) NULL,
ADD FOREIGN KEY Campaign_update_request_id_fk (update_request_id) REFERENCES UpdateRequest(update_request_id);
