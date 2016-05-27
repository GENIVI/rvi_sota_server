#!/bin/bash
set -eu

if [[ ! -d "entrypoint.d" ]]; then
    mkdir "entrypoint.d"
fi

docker rm --force mysql-sota || true

echo "
CREATE DATABASE sota_resolver;
CREATE DATABASE sota_resolver_test;
CREATE DATABASE sota_core;
CREATE DATABASE sota_core_test;
CREATE DATABASE sota_device_registry;
CREATE DATABASE sota_device_registry_test;
GRANT ALL PRIVILEGES ON \`sota\_core%\`.* TO '$CORE_DB_USER'@'%';
GRANT ALL PRIVILEGES ON \`sota\_resolver%\`.* TO '$CORE_DB_USER'@'%';
GRANT ALL PRIVILEGES ON \`sota\_device\_registry%\`.* TO '$CORE_DB_USER'@'%';
FLUSH PRIVILEGES;
" > entrypoint.d/db_user.sql

docker run -d \
  --name mysql-sota \
  -p 3306:3306 \
  -v $(pwd)/entrypoint.d:/docker-entrypoint-initdb.d \
  -e MYSQL_ROOT_PASSWORD=sota-test \
  -e MYSQL_USER="$CORE_DB_USER" \
  -e MYSQL_PASSWORD="$CORE_DB_PASSWORD" \
  mariadb:10.1 \
  --character-set-server=utf8 --collation-server=utf8_unicode_ci \
  --max_connections=1000

function wait_mysql {
    docker run -i --link mysql-sota:mysql --rm --name mysql-ping mariadb \
           sh -c 'exec mysqladmin ping -h"$MYSQL_PORT_3306_TCP_ADDR" -P"$MYSQL_PORT_3306_TCP_PORT" -uroot -p"$MYSQL_ENV_MYSQL_ROOT_PASSWORD"'
}

until wait_mysql; do
    echo "Waiting for mariadb"
    sleep 0.2
done
