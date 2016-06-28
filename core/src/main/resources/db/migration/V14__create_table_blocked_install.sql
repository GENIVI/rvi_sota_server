CREATE TABLE BlockedInstall (
  uuid char(36) NOT NULL,
  blocked_at DATETIME NULL,

  PRIMARY KEY (uuid)
);
