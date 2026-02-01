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

   # Installed package (Linux)
   gop server -config /opt/gop/config/mysql.json

   # Source checkout
   gop server -config conf/mysql.json

3) Console mode (run)
---------------------

::

   # Installed package (Linux)
   gop run -config /opt/gop/config/mysql.json -interval 1

   # Source checkout
   gop run -config conf/mysql.json -interval 1

4) Read logs (watch)
--------------------

After logs are written, list sources and watch:

::

   gop ls
   gop ls <config>
   gop ls <config>/<source>/2026

   gop watch -config <config> -source <sourceId> -head 5
   gop watch -config <config>

Log path pattern:

::

   <logPath>/<configName>/YYYY/MM/<source>/log_YYYYMMDD.json
