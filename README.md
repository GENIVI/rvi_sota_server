# GENIVI SOTA Project

This project is the top-level git repository for the GENIVI SOTA project.

Please refer to [the documentation](http://advancedtelematic.github.io/rvi_sota_server/) for more information.

## Running tests

### Setup

To run tests, we need a mariadb instance running. We also need a user
with `CREATE DATABASE` privileges. All tests run in parallel, so we
need to increaser maximum allowed connections with:

    set global max_connections = 1000;
    
The database also needs to be started with a default encoding and
collation. This corresponds to the `--character-set-server=utf8
--collation-server=utf8_unicode_ci` flags.

This can be done with the following:

    mkdir entrypoint.d/

    echo "
    CREATE DATABASE sota_resolver;
    CREATE DATABASE sota_resolver_test;
    CREATE DATABASE sota_core;
    CREATE DATABASE sota_core_test;
    GRANT ALL PRIVILEGES ON \`sota\_core%\`.* TO 'sota'%';
    GRANT ALL PRIVILEGES ON \`sota\_resolver%\`.* TO 'sota'%';
    set global max_connections = 1000;
    FLUSH PRIVILEGES;
    " > entrypoint.d/db_user.sql
    
    docker run -d \
      --name mariadb-sota \
      -p 3306:3306 \
      -v $(pwd)/entrypoint.d:/docker-entrypoint-initdb.d \
      -e MYSQL_ROOT_PASSWORD=sota-test \
      -e MYSQL_USER=sota_test \
      -e MYSQL_PASSWORD=s0ta \
      mariadb:10.1 --character-set-server=utf8 --collation-server=utf8_unicode_ci

After being created, the database can be started and stopped with
`docker start/stop mariadb-sota`

### Running

There are multiple test tasks that can be executed:

* `sota-core/ut:test` Sota core unit tests

* `sota-core/it:test` Sota core integration tests. This requires an
  rvi server running.
  
* `sota-core/test` Runs all sota core tests

* `sota-webserver/test` Runs all sota webserver tests. This requires
  sota-core and sota-resolver instances running, as well as an rvi
  server.
    
* `sota-resolver/ut:test` Resolver unit tests

* `sota-resolver/rd:test` Runs the random test generator for resolver

* `sota-resolver/test` Runs all resolver tests

