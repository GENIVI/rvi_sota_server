#!/bin/bash
set -eu

docker-compose -p sota -f docker-compose/server-bare.yml down &
docker-compose -p sota -f docker-compose/server-rvi.yml down &
docker-compose -p sota -f docker-compose/server-rvi-client.yml down &
