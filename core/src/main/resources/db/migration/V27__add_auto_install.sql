CREATE TABLE `AutoInstall` (
  `namespace` char(200) NOT NULL,
  `pkg_name` varchar(200) NOT NULL,
  `device` char(36) NOT NULL,

  PRIMARY KEY (namespace, pkg_name, device)
);
