
ALTER TABLE UpdateRequest ADD namespace CHAR(200) NULL AFTER update_request_id;

update  UpdateRequest ur, Package p
set ur.namespace = p.namespace
where ur.package_uuid = p.uuid and ur.namespace is null
;

ALTER TABLE UpdateRequest MODIFY namespace CHAR(200) NOT NULL ;
