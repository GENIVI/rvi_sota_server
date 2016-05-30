#!/bin/bash
set -eu

docker-compose -p sota -f docker-compose/server-bare.yml down &
