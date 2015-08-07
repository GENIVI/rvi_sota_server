# GENIVI SOTA Project

Welcome to the GENIVI SOTA wiki! This is the hub for documentation of all the components of the GENIVI SOTA project.

In this repository you will find all the code for the SOTA Server, comprised of Core, Web Server and External Resolver modules.

This project also houses common resources for the sub-projects including:

 - Complete [Architectural documentation](Architecture)
 - A [Style Guide](Scala-Styleguide)
 - [Development process documentation](#process)
 - [Installation / deployment documentation and scripts](#install_deploy)

## <a name="install_deploy">Deployment / Installation</a>

<!--- The [sota_client](https://github.com/advancedtelematic/sota-client) project builds an RPM that can be installed on a target system, and includes Yocto recipes to allow it to be built into a GENIVI Demo Platform or AGL Reference Platform image.
-->

For the server-side components, this project includes a [docker-laucher](https://github.com/advancedtelematic/docker-launcher) configuration file to allow the cluster of components to be deployed conveniently to a developer machine. In future versions, it will also include docker-launcher configuration files to deploy to an IaaS cloud (e.g. AWS).

See [Deployment with Docker Launcher](Deployment-with-Docker-Launcher) for deploying development or production systems with Docker Launcher.

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

Once flywayMigrate has run, open three consoles and run:

    sbt core/run
    sbt resolver/run
    CORE_HOST=localhost RESOLVER_HOST=localhost sbt webserver/run

Now open [localhost:9000](http://localhost:9000/) in a browser.

## Contributing

This project is developed entirely in the open, on public mailing lists and with public code reviews. To participate in development discussions, please subscribe to the [automotive-eg-rvi](https://lists.linuxfoundation.org/mailman/listinfo/automotive-eg-rvi) mailing list, or join the #automotive channel on Freenode. Code is reviewed on [gerrit](https://gerrithub.io). Development is planned and issues are tracked in [JIRA](https://www.atlassian.com/software/jira).

All code contributed to this project must be licensed under the [MPL v2 license](https://www.mozilla.org/MPL/2.0/), a copy of which you can find in this repository. Documentation must be licensed under the [CC BY 4.0 license](https://creativecommons.org/licenses/by/4.0/).

### <a name="style">Coding Style</a>

You can find documentation of the expected <a href="Scala-Styleguide">Scala code style on the Wiki</a>.

### <a name="process">Development Process</a>

This project is developed with a special focus on secure engineering. In the *docs* folder you will find details of the security architecture and threat model.

During development, any interaction between components must be documented and included in the security modelling. To this end, each project includes a list of implemented requirements and permitted interactions.

Developers must only implement functionality for which there is an associated requirement, described in the project JIRA. When implementing functionality, developers must update the list of [implemented requirements](Requirements). Developers must only implement interactions that are permitted or whitelisted according to the associated JIRA ticket. The list of [Whitelisted Interactions](Whitelisted-Interactions) should be updated when new functionality is implemented, and reviewers should ensure that the code only implements permitted interactions.

## Database Migrations

Never make changes to migrations that already exist. Add columns by creating a new migration with an 'ALTER TABLE' statement.

If someone else has added a migration, run `sbt core/flywayMigrate` to update your local database.


## Database code style

### Table names are UpperCamelCase Singular.

Table names should be the same as the Scala domain object that represents them (if it exists). By using the same casing rules as Scala, the domain object and SQL table names can match exactly.

For more arguments on the singular/plural naming, see http://stackoverflow.com/questions/338156/table-naming-dilemma-singular-vs-plural-names

### Column names are lowerCamelCase

Lower camel case column names should match scala property names.

### Surrogate primary keys are called 'id'.

For example:

```sql
    -- Good
    CREATE TABLE Person (
      id int PRIMARY KEY,
      ...
    );
```

rather than:

```sql
    -- BAD
    CREATE TABLE Person (
      personId int PRIMARY KEY, -- BAD: should be 'id'
      ...
    );
```

## Editing this Wiki 

This wiki constitutes the official documentation for the GENIVI SOTA project. As such, any changes need to be approved via pull request, not via direct edits to the wiki. Unfortunately, GitHub does not support pull requests directly on wikis. The canonical copy of this wiki lies in the main project repository, under `docs/wiki`. **Any changes to documentation that need to be made should be made on the files there, then submitted to the main project as a pull request.** When the pull request is merged, the wiki will get updated.

