#!/usr/bin/env bash

# Pull latest version
git -C ../pam-docker-compose-shared pull

# Start shared docker compose services (must be sourced, or DOCKER_COMPOSE_COMMAND won't be available)
. ../pam-docker-compose-shared/start-docker-compose.sh postgres

# Create database
../pam-docker-compose-shared/create-database.sh "pam-emailer"

$DOCKER_COMPOSE_COMMAND up --remove-orphans
