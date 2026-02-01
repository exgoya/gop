Config
======

Schema file: ``docs/config.schema.json``

Log path rule
-------------

Logs are stored under:

::

   <logPath>/<configName>/YYYY/MM/<source>/log_YYYYMMDD.json

``configName`` defaults to the config filename (without extension).
A copy of the config is stored as ``config.json`` under that folder when
server mode starts. If the same name exists but content differs, the
existing folder is backed up before a new one is created.

Linux package defaults:

- ``fileLog.logPath`` = ``/var/lib/gop/``
- ``api.logPath`` = ``/var/log/gop/api.log``

Samples
-------

- Installed packages:

  - ``/etc/gop/mysql.json`` (Linux)
  - ``/etc/gop/mariadb.json``
  - ``/etc/gop/postgres.json``
  - ``/etc/gop/oracle.json``
  - ``/etc/gop/multi.json``
  - ``/etc/gop/config.json`` (default, MySQL)

- Source checkout:

  - ``conf/mysql.json``
  - ``conf/mariadb.json``
  - ``conf/postgres.json``
  - ``conf/oracle.json``
  - ``conf/multi.json``

Key fields
----------

``setting``
^^^^^^^^^^^

- ``jdbcSource`` (required)
- ``source`` (optional)
- ``timeInterval`` (required)
- ``consolePrint`` (required)
- ``pageSize`` (required)
- ``retention`` (required)
- ``printCSV`` (required)
- ``fileLog`` (required)
  - ``enable``
  - ``logPath``
  - ``maxBytes`` (optional)
  - ``maxBackups`` (optional)
- ``api`` (optional)
  - ``enable``
  - ``port``
  - ``threadPoolSize``
  - ``logPath``

``measure[]``
^^^^^^^^^^^^^

- ``name`` (required)
- ``sql`` (required)
- ``tag`` (optional)
- ``diff`` (optional)
- ``alertValue`` (optional)
- ``alertPolicy`` (optional)
- ``alertSql`` (optional)
- ``sqlIsOs`` (optional)
- ``alertScript`` (optional)
- ``alertScriptIsOs`` (optional)

``sources[]``
^^^^^^^^^^^^^

Use ``sources`` instead of ``measure`` for multi-source monitoring.
Each source runs in its own thread.

Source id rule
--------------

``A-Za-z0-9._-`` only (no spaces/special characters).
