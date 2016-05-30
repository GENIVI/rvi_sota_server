#!/bin/bash
set -eu

docker-compose -p sota -f docker-compose/server-bare.yml pull
docker-compose -p sota -f docker-compose/server-rvi.yml  pull
docker-compose -p sota -f docker-compose/server-rvi-client.yml pull
