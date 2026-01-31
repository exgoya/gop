Local API
=========

The API is available in server mode on ``127.0.0.1``.
It reads log files and does not query the DB directly.

Health
------

::

   GET /health

Status
------

::

   POST /status
   {"source":"mysql-local","name":"threads_connected"}

Fields:

- ``source`` (optional)
- ``name`` / ``tag`` (optional)

Watch
-----

::

   POST /watch
   {"source":"mysql-local","timeFrom":"2026-01-31 00:00:00.000","timeTo":"2026-01-31 23:59:59.999"}

Fields:

- ``source`` (required)
- ``timeFrom`` / ``timeTo`` (optional, ``yyyy-MM-dd HH:mm:ss.SSS``)
- ``name`` / ``tag`` (optional)
- ``head`` / ``tail`` (optional)
