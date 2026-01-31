Config
======

Schema file: ``docs/config.schema.json``

Log path rule
-------------

Logs are stored under:

::

   <logPath>/<configId>/YYYY/MM/<source>/log_YYYYMMDD.json

If ``configId`` is not set, the config filename (without extension) is used.
A copy of the config is stored as ``config.json`` in the same directory when
server mode starts.

Samples
-------

- ``data/config-mysql.json``
- ``data/config-mariadb.json``
- ``data/config-postgres.json``
- ``data/config-oracle.json``
- ``data/config-multi.json``

Key fields
----------

``setting``
^^^^^^^^^^^

- ``jdbcSource`` (required)
- ``source`` (optional)
- ``configId`` (optional)
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
