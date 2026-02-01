Functional Specification
========================

Overview
--------

``gop`` is a console tool for database monitoring. It periodically runs
configured queries against one or more sources, stores results as NDJSON,
and can print live summaries or query stored logs.

Goals
-----

- Collect metrics from JDBC sources (single or multi-source).
- Persist results to local log files for later inspection.
- Provide a local HTTP API for log queries (no direct DB access).
- Offer a CLI for listing and watching logs.

Non-goals
---------

- No distributed storage or clustering.
- No remote API access (API binds to localhost).
- No built-in authentication/authorization.

Operating modes
---------------

Server
^^^^^^

- Collects metrics and writes log files.
- Starts local HTTP API (if enabled).
- No continuous console output (startup info only).

Run
^^^

- Console-only, sar-style output.
- Does not write log files.
- Uses a fixed interval (``timeInterval`` or ``-interval``).

Watch
^^^^^

- Reads stored logs and prints formatted output.
- Supports filtering by time range, measure name, or tag.
- ``-follow`` streams new log lines (tail -f style).

List (ls)
^^^^^^^^^

- Lists configs/sources/years/months/log files under the log root.

Configuration model
-------------------

- ``setting``
  - ``jdbcSource``: JDBC connection information.
  - ``source``: source id (single-source mode).
  - ``timeInterval``: polling interval (ms).
  - ``consolePrint``: print to console (run mode).
  - ``pageSize``: header repeat interval.
  - ``retention``: log retention policy.
  - ``printCSV``: CSV output flag.
  - ``fileLog``: log path and rotation settings.
  - ``api``: local API settings.
- ``measure[]``
  - Query definitions, tags, and alert rules.
- ``sources[]`` (multi-source mode)
  - Per-source ``jdbcSource`` + ``measure``.

Log storage
-----------

Path pattern:

::

   <logPath>/<configName>/YYYY/MM/<source>/log_YYYYMMDD.json

Where:

- ``configName`` = config filename without extension.
- ``source`` = ``source`` id from config.

Log format
^^^^^^^^^^

- NDJSON (one JSON object per line).
- Each line contains:
  - ``time`` (``yyyy-MM-dd HH:mm:ss.SSS``)
  - ``source``
  - ``rc`` array (measure, value, tag, alert)

Config snapshot
^^^^^^^^^^^^^^^

- On server start, a copy of the config is saved to:
  ``<logPath>/<configName>/config.json``.
- If the same name exists but content differs, the existing folder is backed
  up with ``_old_YYYYMMDDHHmmss`` suffix.

Rotation
^^^^^^^^

- Log files are rotated when ``maxBytes`` is exceeded.
- A compressed ``.gz`` backup is kept up to ``maxBackups``.

Alerts
------

- If a measure triggers an alert, the configured script runs.
- Alert output is appended to:
  ``<logPath>/<configName>/YYYY/MM/<source>/alert_YYYYMM.json``.

CLI behavior
------------

ls
^^

::

   gop ls
   gop ls <config>
   gop ls <config>/<source>
   gop ls <config>/<source>/YYYY
   gop ls <config>/<source>/YYYY/MM

watch
^^^^^

::

   gop watch -config <config> -source <sourceId>
   gop watch -config <config>            # all sources
   gop watch -config <config> -source <sourceId> tail -follow

Notes:

- ``-source`` is optional. If omitted, all sources in the config are printed.
- If ``-config`` is omitted and multiple configs exist, ``watch`` lists them.
- ``-follow`` requires ``-source``.
- ``-time`` and ``-follow`` are mutually exclusive.

API
---

- Local-only (``127.0.0.1``).
- Reads log files only; no DB queries.

Endpoints:

- ``GET /health``
- ``POST /status``
- ``POST /watch``

Limitations
-----------

- ``-follow`` follows the current log file only. If a new day starts,
  restart watch to follow the new file.
- ``watch`` output depends on existing log files; no DB backfill.

