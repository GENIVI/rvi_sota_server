#!/bin/bash

if [[ "$#" -lt "1" ]]; then
 echo "usage: $0 <service_name> [service args]"
 exit 1
fi

SERVICE_NAME=$1

shift

if [ -z "$DB_ALIVE_URL" ]; then
	exec bin/$SERVICE_NAME $@
else
	./wait-for-it.sh $DB_ALIVE_URL --strict --timeout=120 && exec bin/$SERVICE_NAME $@
fi
