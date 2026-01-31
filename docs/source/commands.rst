Commands
========

Basic usage
-----------

::

   gop <server|run|watch> -config <config file path> [options]

Version
-------

::

   gop version
   gop -version

Modes
-----

- ``server``: collect + write logs + API
- ``run``: console output only (sar style)
- ``watch``: query saved logs

Run options
-----------

::

   -interval <sec>
   -interval-ms <ms>

Watch options
-------------

::

   -log <log file path>
   -time 'yyyy-mm-dd hh24:mi:ss.fff' 'yyyy-mm-dd hh24:mi:ss.fff'
   -name <column name>
   -tag <tag name>
   -head <count>
   -tail <count>
