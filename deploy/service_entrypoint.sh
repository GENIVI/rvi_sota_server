#!/bin/bash

if [[ "$#" -lt "1" ]]; then
 echo "usage: $0 <service_name> [service args]"
 exit 1
fi

_service_name=$1

shift

if [ -z "$DB_ALIVE_URL" ]; then
	exec bin/$_service_name $@
else
	./wait-for-it.sh $DB_ALIVE_URL --strict --timeout=120 && exec bin/$_service_name $@
fi
