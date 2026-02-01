Commands
========

Basic usage
-----------

::

   gop server -config <config file path> [options]
   gop run -config <config file path> [options]
   gop ls [<config>[/<source>[/YYYY[/MM]]]] [-path <log root>]
   gop watch [-config <config name>] [-source <sourceId>] [options]

Version
-------

::

   gop version
   gop -version

Modes
-----

- ``server``: collect + write logs + API
- ``run``: console output only (sar style)
- ``ls``: list configs/sources/years/months/files
- ``watch``: query saved logs (auto-discover log files)

Run options
-----------

::

   -interval <sec>
   -interval-ms <ms>

Watch options
-------------

::

   -config <config name>
   -source <sourceId>
   -time 'yyyy-mm-dd hh24:mi:ss.fff' 'yyyy-mm-dd hh24:mi:ss.fff'
   -name <column name>
   -tag <tag name>
   -head <count>
   -tail <count>
   -f <log file path>
   -follow | -F
   -path <log root path>
   -csv

The default log root is ``data/``. You can also set ``GOP_LOG_PATH``.
If no ``-head``/``-tail`` and no ``-time`` are provided, watch shows the latest 20 rows.
The optional ``tail`` token (e.g., ``gop watch -source mysql-local tail``) is accepted.
If the same source exists under multiple config folders, watch picks the most recent one.
Use ``-follow`` to stream new log lines (tail -f style).
If ``-source`` is omitted, watch prints all sources under the selected config.

List options
------------

::

   gop ls
   gop ls <config>
   gop ls <config>/<source>
   gop ls <config>/<source>/YYYY
   gop ls <config>/<source>/YYYY/MM

Option:

``-path <log root path>`` (default: ``data/`` or ``GOP_LOG_PATH``)
