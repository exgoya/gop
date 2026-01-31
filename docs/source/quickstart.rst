Quickstart (MySQL)
==================

This quickstart uses the bundled Docker compose to run MySQL locally.

1) Start the database
---------------------

::

   docker compose -f docker/docker-test.yml up -d

2) Start monitoring (server)
----------------------------

::

   gop server -config data/config-mysql.json

3) Console mode (run)
---------------------

::

   gop run -config data/config-mysql.json -interval 1

4) Read logs (watch)
--------------------

After logs are written, point watch to a log file:

::

   gop watch -config data/config-mysql.json -log <log_file> -head 5

Log path pattern:

::

   <logPath>/<configId>/YYYY/MM/<source>/log_YYYYMMDD.json
