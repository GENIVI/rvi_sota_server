#!/bin/bash

set -e
set -u

if [[ $# != 6 ]]; then
    echo "usage: $0 host port user password interval tries"
    exit 1
fi

HOST=$1; shift
PORT=$1; shift
USER=$1; shift
PASS=$1; shift
TIMEOUT=$1; shift
TRIES=$1; shift

(mysqladmin --version > /dev/null) || {
    echo "error: mysqladmin command is not installed"
    exit 1
}

for t in `seq $TRIES`; do
    res=$(mysqladmin ping --protocol=TCP -h $HOST -P $PORT -u $USER -p$PASS || true)
    if [[ $res =~ "mysqld is alive" ]]; then
        echo "mysql is ready"
        exit 0
    else
        echo "Waiting for mariadb"
        sleep $TIMEOUT
    fi
done
