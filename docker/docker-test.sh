#!/usr/bin/env bash
set -euo pipefail

docker compose -f docker/docker-test.yml up -d
trap 'docker compose -f docker/docker-test.yml down' EXIT

export RUN_DOCKER_TESTS=1
./gradlew test
