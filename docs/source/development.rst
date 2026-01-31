Development
===========

Build
-----

::

   ./gradlew build

Tests
-----

::

   ./gradlew test

Docker integration test
-----------------------

::

   docker compose -f docker/docker-test.yml up -d
   ./docker/docker-test.sh

Only runs when ``RUN_DOCKER_TESTS=1``.
