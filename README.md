# GENIVI SOTA Project

This project is the top-level git repository for the GENIVI SOTA project. The project is comprised of four sub-projects:

 - core - The central component of the SOTA server that implements package queuing and distribution
 - external-resolver - An implementation of a package resolution server, which maps VINs to a list of software components to be installed on a vehicle
 - web-server - The web interface to the SOTA server, allowing administration of vehicles and software components
 - sota_client - The vehicle-resident SOTA download client

In combination, these four projects represent a complete implementation of a basic software-over-the-air update system for automotive.

This top-level project also houses common resources for the sub-projects including:

 - Architectural documentation
 - Style Guides
 - Development process documentation
 - Installation / deployment documentation and scripts

## Deployment / Installation

The [sota_client](https://github.com/advancedtelematic/sota_client) project builds an RPM that can be installed on a target system, and includes Yocto recipes to allow it to be built into a GENIVI Demo Platform or AGL Reference Platform image.

For the server-side components, this project includes a [docker_laucher](https://github.com/advancedtelematic/docker_launcher) configuration file to allow the cluster of components to be deployed conveniently to a developer machine or IaaS cloud (e.g. AWS)

See [docs/docker-launcher.md](https://github.com/advancedtelematic/sota-server/master/docs/docker-launcher.md) for deploying developement or production systems with Docker Launcher

## Contributing

This project is developed entirely in the open, on public mailing lists and with public code reviews. To participate in development discussions, please subscribe to the [automotive-eg-rvi](https://lists.linuxfoundation.org/mailman/listinfo/automotive-eg-rvi) mailing list, or join the #automotive channel on Freenode. Code is reviewed on [gerrit](https://gerrithub.io). Development is planned and issues are tracked in [JIRA](https://www,atlassian.com/software/jira).

All code contributed to this project must be licensed under the MPL v2 license, a copy of which you can find in this repository. Documentation must be licensed under the CC 4.0 license.

### Coding Style

You can find documentation of the expected Scala code style in the *docs* folder of this project.

### Development Process

This project is developed with a special focus on secure engineering. In the *docs* folder you will find details of the security architecture and threat model.

During development, any interaction between components must be documented and included in the security modelling. To this end, each project includes a list of implemented requirements and permitted interactions.

Developers must only implement functionality for which there is an associated requirement, described in the project JIRA. When implementing functionality, developers must update the list of implemented requirements (*docs/requirements.md*). Developers must only implement interactions that are permitted or whitelisted according to the associated JIRA ticket. The list of permitted interactions (*docs/permitted-interactions.md*) should be updated when new functionality is implemented, and reviewers should ensure that the code only implements permitted interactions.

## Database setup

For development, a local MariaDB install is required. Create a new database called 'sota':

    mysql -u root -p
    CREATE DATABASE sota;
    CREATE USER 'sota'@'localhost' IDENTIFIED BY 's0ta';
    GRANT ALL PRIVILEGES ON sota . * TO 'sota'@'localhost';
    FLUSH PRIVILEGES;

To update the database schema, run:

    sbt core/flywayMigrate

This will apply any new migrations in src/main/resources/db/migration, and keep your existing data.


## Database Migrations

Never make changes to migrations that already exist. Add columns by creating a new migration with an
'ALTER TABLE' statement.

If someone else has added a migration, run `sbt core/flywayMigrate` to update your local database.


## Database code style

### Table names are UpperCamelCase Singular.

Table names should be the same as the Scala domain object that represents them (if it exists). By using the same
casing rules as Scala, the domain object and SQL table names can match exactly.

For more arguments on the singular/plural naming, see
http://stackoverflow.com/questions/338156/table-naming-dilemma-singular-vs-plural-names

### Column names are lowerCamelCase

Lower camel case column names should match scala property names.

### Surrogate primary keys are called 'id'.

For example:

    -- Good
    CREATE TABLE Person (
	    id int PRIMARY KEY,
	    ...
    );

rather than:

    -- BAD
    CREATE TABLE Person (
	    personId int PRIMARY KEY, -- BAD: should be 'id'
	    ...
    );
