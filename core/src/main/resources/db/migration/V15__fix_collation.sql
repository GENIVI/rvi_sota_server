ALTER DATABASE CHARACTER SET utf8 COLLATE utf8_unicode_ci;

SET foreign_key_checks = 0;

ALTER TABLE BlockedInstall CONVERT TO CHARACTER SET utf8 COLLATE utf8_unicode_ci ;
ALTER TABLE InstallHistory CONVERT TO CHARACTER SET utf8 COLLATE utf8_unicode_ci ;
ALTER TABLE OperationResult CONVERT TO CHARACTER SET utf8 COLLATE utf8_unicode_ci ;
ALTER TABLE Package CONVERT TO CHARACTER SET utf8 COLLATE utf8_unicode_ci ;
ALTER TABLE RequiredPackage CONVERT TO CHARACTER SET utf8 COLLATE utf8_unicode_ci ;
ALTER TABLE UpdateRequest CONVERT TO CHARACTER SET utf8 COLLATE utf8_unicode_ci ;
ALTER TABLE UpdateSpec CONVERT TO CHARACTER SET utf8 COLLATE utf8_unicode_ci ;

SET foreign_key_checks = 1;

