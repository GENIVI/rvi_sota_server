-- Create some sample data

INSERT INTO Vin (vin) VALUES ('VIN001');
INSERT INTO Vin (vin) VALUES ('VIN002');
INSERT INTO Vin (vin) VALUES ('VIN003');

INSERT INTO Package (name, version, description, vendor)
VALUES ('libc6', '2.20', 'Contains the standard libraries that are used by nearly all programs on the system.', 'GNU Libc Maintainers');

INSERT INTO Package (name, version, description, vendor)
VALUES ('vim', '2.7.4.052', 'The world''s best text editor.', 'Bram Moolenaar');


INSERT INTO InstallRequest (packageId, priority, startAfter, endBefore)
SELECT id, 34, '1970-01-01', '2015-12-31' FROM Package WHERE name='libc6' AND VERSION = '2.20';
