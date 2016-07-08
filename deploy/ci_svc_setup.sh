#!/bin/bash

##
# Tool to start/stop services for CI testing
##

cd $(git rev-parse --show-toplevel)/deploy/docker-compose/
case $1 in
  "up")
    docker-compose -p sota up -d
    ;;
  "down")
    docker-compose -p sota down
    docker-compose -p sota rm -fv
    docker network rm sota_default
    docker rm --force mysql-sota

    docker images \
      | awk '/advancedtelematic\/sota-(core|resolver|webserver|device)/ { print $3 }' \
      | xargs docker rmi \
      || true
    ;;
  *)
    echo "Usage: $0 up|down"
    exit 1
    ;;
esac
