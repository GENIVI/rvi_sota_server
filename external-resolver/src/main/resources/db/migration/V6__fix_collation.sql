ALTER DATABASE CHARACTER SET utf8 COLLATE utf8_unicode_ci;

SET foreign_key_checks = 0;

ALTER TABLE Component CONVERT TO CHARACTER SET utf8 COLLATE utf8_unicode_ci;
ALTER TABLE Filter CONVERT TO CHARACTER SET utf8 COLLATE utf8_unicode_ci;
ALTER TABLE Firmware CONVERT TO CHARACTER SET utf8 COLLATE utf8_unicode_ci;
ALTER TABLE InstalledComponent CONVERT TO CHARACTER SET utf8 COLLATE utf8_unicode_ci;
ALTER TABLE InstalledPackage CONVERT TO CHARACTER SET utf8 COLLATE utf8_unicode_ci;
ALTER TABLE Package CONVERT TO CHARACTER SET utf8 COLLATE utf8_unicode_ci;
ALTER TABLE PackageFilter CONVERT TO CHARACTER SET utf8 COLLATE utf8_unicode_ci;

SET foreign_key_checks = 1;

