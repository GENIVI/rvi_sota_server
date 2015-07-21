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
