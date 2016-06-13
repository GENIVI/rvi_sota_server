#!/bin/bash

set -e
set -u

# Converts:
# jdbc:mariadb://localhost:3306/sota_core => localhost 3306
function parse_jdbc_url {
    echo $1 | sed -r -e 's/^.+\/\/(.+):([0-9]+).+/\1 \2/'
}

cd /migrations

CORE_DB_MIGRATE=${CORE_DB_MIGRATE:-}
RESOLVER_DB_MIGRATE=${RESOLVER_DB_MIGRATE:-}

MYSQL=$(parse_jdbc_url $CORE_DB_URL)

./wait_for_mysql.sh $MYSQL $CORE_DB_USER $CORE_DB_PASS 1 60

git -C rvi_sota_server pull

if [[ "$CORE_DB_MIGRATE" == "true" ]]; then
    flyway/flyway -url=$CORE_DB_URL \
              -user=$CORE_DB_USER -password=$CORE_DB_PASS \
              -locations=filesystem:./rvi_sota_server/core/src/main/resources/db/migration \
              migrate
else
    echo -e "\e[31;1m\$CORE_DB_MIGRATE false, not migrating\e[0m"
fi

if [[ "$RESOLVER_DB_MIGRATE" == "true" ]]; then
    echo "$RESOLVER_DB_MIGRATE false, not migrating"
    flyway/flyway -url=$RESOLVER_DB_URL \
              -user=$RESOLVER_DB_USER -password=$RESOLVER_DB_PASS \
              -locations=filesystem:./rvi_sota_server/external-resolver/src/main/resources/db/migration \
              migrate
else
    echo -e "\e[31;1m\$RESOLVER_DB_MIGRATE false, not migrating\e[0m"
fi
    
flyway/flyway -url=$CORE_DB_URL \
              -user=$CORE_DB_USER -password=$CORE_DB_PASS \
              -locations=filesystem:./rvi_sota_server/core/src/main/resources/db/migration \
              info

flyway/flyway -url=$RESOLVER_DB_URL \
              -user=$RESOLVER_DB_USER -password=$RESOLVER_DB_PASS \
              -locations=filesystem:./rvi_sota_server/external-resolver/src/main/resources/db/migration \
              info
