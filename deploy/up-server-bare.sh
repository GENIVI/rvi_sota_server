#!/bin/bash
set -eu

docker-compose -p sota -f docker-compose/server-bare.yml up -d mysql &
sleep 120
docker-compose -p sota -f docker-compose/server-bare.yml up &

