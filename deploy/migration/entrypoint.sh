#!/bin/bash

set -e
set -u

# Converts:
# jdbc:mariadb://localhost:3306/sota_core => localhost 3306
function parse_jdbc_url {
    echo $1 | sed -r -e 's/^.+\/\/(.+):([0-9]+).+/\1 \2/'
}


MIGRATIONS_WORKDIR=${MIGRATIONS_WORKDIR:-/migrations}
CLEAN_ALL=${CLEAN_ALL:-false}
CORE_DB_MIGRATE=${CORE_DB_MIGRATE:-}
RESOLVER_DB_MIGRATE=${RESOLVER_DB_MIGRATE:-}
DEVICE_REGISTRY_DB_MIGRATE=${DEVICE_REGISTRY_DB_MIGRATE:-}

MYSQL=$(parse_jdbc_url $CORE_DB_URL)

cd $MIGRATIONS_WORKDIR

./wait_for_mysql.sh $MYSQL $CORE_DB_USER $CORE_DB_PASS 1 60

git -C rvi_sota_server pull


function core_flyway() {
        flyway/flyway -url=$CORE_DB_URL \
              -user=$CORE_DB_USER -password=$CORE_DB_PASS \
              -locations=filesystem:./rvi_sota_server/core/src/main/resources/db/migration \
              $*
}

function resolver_flyway() {
        flyway/flyway -url=$RESOLVER_DB_URL \
              -user=$RESOLVER_DB_USER -password=$RESOLVER_DB_PASS \
              -locations=filesystem:./rvi_sota_server/external-resolver/src/main/resources/db/migration \
              $*
}

function device_registry_flyway() {
    flyway/flyway -url=$DEVICE_REGISTRY_DB_URL \
                  -user=$DEVICE_REGISTRY_DB_USER -password=$DEVICE_REGISTRY_DB_PASS \
                  -locations=filesystem:./rvi_sota_server/device-registry/src/main/resources/db/migration \
                  $*
}


if [[ "$CLEAN_ALL" == "true" ]]; then
    read -p "Are you sure you want to clean the database? [YES/no]" prompt

    if [[ "$prompt" == "YES" ]]; then
        core_flyway clean
        resolver_flyway clean
    else
        echo "clean cancelled"
    fi
fi


if [[ "$CORE_DB_MIGRATE" == "true" ]]; then
    core_flyway migrate
else
    echo -e "\e[31;1m\$CORE_DB_MIGRATE false, not migrating\e[0m"
fi

if [[ "$RESOLVER_DB_MIGRATE" == "true" ]]; then
    echo "$RESOLVER_DB_MIGRATE false, not migrating"
    resolver_flyway migrate
else
    echo -e "\e[31;1m\$RESOLVER_DB_MIGRATE false, not migrating\e[0m"
fi
    
if [[ "$DEVICE_REGISTRY_DB_MIGRATE" == "true" ]]; then
    device_registry_flyway migrate
else
    echo -e "\e[31;1m\$DEVICE_REGISTRY_DB_MIGRATE false, not migrating\e[0m"
fi

core_flyway info

resolver_flyway info

device_registry_flyway info
