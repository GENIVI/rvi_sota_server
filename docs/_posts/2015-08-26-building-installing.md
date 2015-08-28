---
layout: page
title: "Building & Deploying"
category: doc
date: 2015-08-26 10:39:45
order: 1
---

<!--- The [sota_client](https://github.com/PDXostc/rvi_sota_client) project builds an RPM that can be installed on a target system, and includes Yocto recipes to allow it to be built into a GENIVI Demo Platform or AGL Reference Platform image.
-->

For the server-side components, this project includes a [docker-laucher](https://github.com/advancedtelematic/docker-launcher) configuration file to allow the cluster of components to be deployed conveniently to a developer machine. In future versions, it will also include docker-launcher configuration files to deploy to an IaaS cloud (e.g. AWS).

See [Deployment with Docker Launcher](../doc/deployment-with-docker-launcher.html) for deploying development or production systems with Docker Launcher.

## Building Locally

For local development, the following prerequisites are required:

1. Java 8
2. mysql (with appropriate databases created)

The other dependencies are managed using Scala's `sbt`. If you have sbt installed then use it, otherwise the `./sbt` script in the root of the project will bootstrap everything you need beyond Java 8.

To check the version of java installed, run:

    # java -version
    java version "1.8.0_45"
    Java(TM) SE Runtime Environment (build 1.8.0_45-b14)
    Java HotSpot(TM) 64-Bit Server VM (build 25.45-b02, mixed mode)

For development, a local MariaDB install is required. (Note that this is **not** required for deployment, as Docker Launcher will handle the creation of the database. If a Docker Launcher local deployment is attempted on a machine already running a database instance, there may be conflicts.) Create two databases called 'sota_core' and 'sota_resolver':

```sql
    mysql -u root -p
    CREATE DATABASE sota_core;
    CREATE DATABASE sota_core_test;
    CREATE DATABASE sota_resolver;
    CREATE DATABASE sota_resolver_test;
    CREATE USER 'sota'@'localhost' IDENTIFIED BY 's0ta';
    GRANT ALL PRIVILEGES ON sota_core . * TO 'sota'@'localhost';
    GRANT ALL PRIVILEGES ON sota_core_test . * TO 'sota'@'localhost';
    GRANT ALL PRIVILEGES ON sota_resolver . * TO 'sota'@'localhost';
    GRANT ALL PRIVILEGES ON sota_resolver_test . * TO 'sota'@'localhost';
    FLUSH PRIVILEGES;
```

To update the database schema, run:

    sbt core/flywayMigrate
    sbt resolver/flywayMigrate

This will apply any new migrations in src/main/resources/db/migration, and keep your existing data.
These commands expect to find the databases on localhost with sota/s0ta for the username/password.
The URL to the database and login details can be overridden with the `CORE_DB_URL`, `CORE_DB_USER` and `CORE_DB_PASSWORD`
environment variables for the core and `RESOLVER_DB_URL`, `RESOLVER_DB_USER` and, `RESOLVER_DB_PASSWORD` for the external resolver.
See `project/SotaBuild.scala` for the implementation.

If you are using an encrypted home directory, you may get the following error when attempting a build. This is because scala/sbt tends to create long file names, and these get expanded even further by ecryptfs.

    [error] File name too long
    [error] one error found
    [error] (core/compile:compileIncremental) Compilation failed
    [error] Total time: 9 s, completed Jul 24, 2015 9:10:13 AM

The solution is to point the build directories to somewhere outside ecryptfs:
 
```
sudo mkdir /var/sota-build
sudo chown `whoami` /var/sota-build/
mkdir /var/sota-build/core-target
mkdir /var/sota-build/resolver-target
mkdir /var/sota-build/webserver-target
rm -r core/target/
rm -r external-resolver/target
rm -r web-server/target
ln -s /var/sota-build/core-target/ core/target
ln -s /var/sota-build/resolver-target external-resolver/target
ln -s /var/sota-build/webserver-target/ web-server/target
```

Once flywayMigrate has run, open three consoles and run:

    sbt core/run
    sbt resolver/run
    CORE_HOST=localhost RESOLVER_HOST=localhost sbt webserver/run

Now open [localhost:9000](http://localhost:9000/) in a browser.


## Database Migrations

Never make changes to migrations that already exist. Add columns by creating a new migration with an 'ALTER TABLE' statement.

If someone else has added a migration, run `sbt core/flywayMigrate` to update your local database.

