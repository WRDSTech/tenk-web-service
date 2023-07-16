#!/bin/bash -e
#
# Script to start docker and update the /etc/hosts file to point to
# the hbase-docker container
#
# hbase thrift and master server logs are written to the local
# logs directory
#

program=$(basename "$0")

echo "$program: Starting Ozone docker cluster"

docker compose -f ozone-cluster-docker-compose.yaml  up -d --scale datanode=3
