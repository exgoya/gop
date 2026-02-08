# gop V2 Plan (Multi-target + Multi-action)

## 1. Goal

- Support one query returning multiple targets.
- Support multiple actions per target (`alert` included).
- Minimize runtime overhead while making alert evaluation explicit and traceable.

## 2. Current Limitations (V1)

- Query result is treated as a single numeric value per `measure`.
- Multiple columns/targets from one row are not first-class.
- Alert model is single rule (`alertPolicy`, `alertValue`) per measure.
- Runtime assumes positional mapping in several places (index-based, not key-based).

## 2.1 Problem -> Support Direction (with SQL examples)

### Case A: One query per target (too many queries)

- Existing problem:
  - To collect `Threads_connected`, `Threads_running`, `Connections`, V1 needs 3 separate `measure` entries and 3 SQL executions.
- V2 direction:
  - One query returns multiple targets, then runtime fans out by `target`.

V1 (separate queries):

```sql
select variable_value
from performance_schema.global_status
where variable_name = 'Threads_connected';
```

```sql
select variable_value
from performance_schema.global_status
where variable_name = 'Threads_running';
```

V2 (single query, long shape):

```sql
select variable_name as target,
       cast(variable_value as signed) as value
from performance_schema.global_status
where variable_name in ('Threads_connected', 'Threads_running', 'Connections');
```

### Case B: Multiple targets in one row (wide row)

- Existing problem:
  - When one row has several target-like metrics, V1 cannot treat each field as independent target/metric.
- V2 direction:
  - Support `resultShape=wide` and fan-out from column mapping.
  - Or normalize to `long` in SQL (recommended).

Wide query example:

```sql
select
  sum(case when variable_name = 'Threads_connected' then cast(variable_value as signed) else 0 end) as threads_connected,
  sum(case when variable_name = 'Threads_running' then cast(variable_value as signed) else 0 end) as threads_running
from performance_schema.global_status;
```

Normalized long query example:

```sql
select 'threads_connected' as target, sum(case when variable_name = 'Threads_connected' then cast(variable_value as signed) else 0 end) as value
from performance_schema.global_status
union all
select 'threads_running' as target, sum(case when variable_name = 'Threads_running' then cast(variable_value as signed) else 0 end) as value
from performance_schema.global_status;
```

### Case C: Only one alert rule/action per metric

- Existing problem:
  - V1 has a single alert rule per measure.
- V2 direction:
  - Multiple actions per target (`warn`, `critical`, script, webhook-ready) with independent conditions and handlers.

Target-specific query source (example):

```sql
select variable_name as target,
       cast(variable_value as signed) as value
from performance_schema.global_status
where variable_name = 'Threads_connected';
```

Target rule condition in config (concept):

- `warn`: `operator=gt`, `threshold=80`
- `critical`: `operator=gt`, `threshold=120` + optional script

## 3. V2 Design Principles

- External config stays readable (array-based, explicit order).
- Internal runtime uses key-indexed maps for fast lookup and consistency.
- Action scope is inherited from parent target unless explicitly overridden.
- Prefer normalized result shape (`target`, `value`) for stable processing.
- Existing script execution must be modeled as a first-class action type (not a special case).
- New notification integrations should be added by action handlers, not by changing metric logic.
- Server runtime settings and source query settings should be separated into different config files.

## 4. Config Model (V2)

## 4.1 Versioning

- Add top-level `schemaVersion`.
- V2 loader accepts only `schemaVersion: 2`.

## 4.2 Server / Source Split Structure

- Separate config into:
  - `server config`: runtime/process settings.
  - `source config`: JDBC/query/action definitions per source.
- Optionally allow monolithic V2 config for simple deployments.

Server config owns:

- polling/runtime: `timeInterval`, `consolePrint`, `pageSize`, `printCSV`.
- storage/api: `fileLog`, `api`.
- source binding: `sourceRefs[]` (source ids or file paths).

Source config owns:

- identity: `source`.
- connection: `jdbcSource`.
- collection rules: `measureV2[]`.

Suggested layout:

- `conf/v2/server/default.json`
- `conf/v2/sources/mysql-local.json`
- `conf/v2/sources/postgres-local.json`

Server config example:

```json
{
  "schemaVersion": 2,
  "server": {
    "timeInterval": 1000,
    "consolePrint": true,
    "printCSV": false,
    "fileLog": { "enable": true, "logPath": "data/" },
    "api": { "enable": true, "port": 18080, "threadPoolSize": 4, "logPath": "data/api.log" },
    "sourceRefs": ["mysql-local", "postgres-local"]
  }
}
```

Source config example:

```json
{
  "schemaVersion": 2,
  "source": "mysql-local",
  "jdbcSource": { "url": "jdbc:mysql://127.0.0.1:3306/", "dbName": "gop", "driverClass": "com.mysql.cj.jdbc.Driver" },
  "measureV2": [
    {
      "name": "global_status",
      "resultShape": "long",
      "sql": "select variable_name as target, cast(variable_value as signed) as value from performance_schema.global_status where variable_name in ('Threads_connected','Threads_running')",
      "targetColumn": "target",
      "valueColumn": "value",
      "targets": [
        {
          "target": "Threads_connected",
          "measure": "threads_connected",
          "viewMode": "raw",
          "rules": [
            { "id": "warn_threshold", "operator": "gt", "threshold": 80, "actions": [{ "name": "warn", "type": "alert" }] }
          ]
        },
        {
          "target": "Threads_running",
          "measure": "threads_running",
          "viewMode": "raw",
          "rules": [
            { "id": "warn_threshold", "operator": "gt", "threshold": 20, "actions": [{ "name": "warn", "type": "alert" }] }
          ]
        }
      ]
    }
  ]
}
```

## 4.3 MeasureV2 (Query Group)

Each `measureV2` defines one query and fan-out rules:

- `name`: query group id.
- `sql`: SQL text.
- `resultShape`: `long` (recommended) or `wide`.
- `targetColumn`: for `long` shape.
- `valueColumn`: for `long` shape.
- `defaultTag`: optional fallback tag.
- `targets[]`: target definitions.

Each target:

- `target`: source-side target key (from DB row/column).
- `measure`: output metric name.
- `viewMode`: value mode (`raw` or `delta`).
- `tag`: optional override.
- `rules[]`: condition rules per target.

Each rule:

- `id`: rule id.
- `operator`: comparison type (`gt`, `lt`, `eq`).
- `threshold`: threshold value.
- `actions[]`: actions to run when the rule condition is true.

Each action:

- `name`: action id.
- `type`: action type (`alert`, `script`, `notify`).
- `script`: optional script body/command.
- `scriptIsOs`: optional script mode.
- `message`: optional template text for notification actions.
- `channel`: optional destination id (`slack`, `teams`, `email`, etc.).
- `endpoint`: optional URL for webhook-like actions.

Example (`rules[]`):

```json
[
  {
    "id": "warn_threshold",
    "operator": "gt",
    "threshold": 80,
    "actions": [
      { "name": "warn", "type": "alert" }
    ]
  },
  {
    "id": "critical_threshold",
    "operator": "gt",
    "threshold": 120,
    "actions": [
      { "name": "critical", "type": "alert" },
      { "name": "critical_script", "type": "script", "script": "echo 'critical'", "scriptIsOs": true },
      { "name": "critical_notify", "type": "notify", "channel": "slack", "message": "[{{source}}] {{measure}}={{value}}" }
    ]
  }
]
```

Note:

- Rule condition (`operator`/`threshold`) is evaluated once per target value.
- All `actions[]` under a matched rule are dispatched.
- Use `rules[]` only in V2 runtime path.

## 4.4 Action Handler Model

- `type=alert`
  - Sets alert state/flags in metric output.
- `type=script`
  - Executes local script/command (current behavior promoted as official action type).
- `type=notify`
  - Sends message through provider adapter (`slack`, `teams`, `email`, `webhook`).

Handler interface direction:

- `ActionHandler` dispatch by `action.type`.
- Provider-specific code isolated under notifier adapters.
- Metric collection path remains unchanged when adding new channels.

## 4.5 Web Registration (Source + Config)

- Provide a local web page for:
  - source config registration/edit/delete.
  - server config registration/edit (runtime, fileLog, api, sourceRefs).
- Keep API bind local-only (`127.0.0.1`) in first phase.

Minimum API set:

- `GET /v2/sources`
- `GET /v2/sources/{sourceId}`
- `POST /v2/sources` (create/update)
- `DELETE /v2/sources/{sourceId}`
- `GET /v2/server-config`
- `POST /v2/server-config` (create/update)
- `POST /v2/config/validate` (syntax + schema + semantic checks)
- `POST /v2/config/reload` (apply updated config without process restart when possible)

Web page requirements:

- Form-based source registration (`source`, `jdbcSource`, `measureV2`, `rules`, `actions`).
- Form-based server registration (`timeInterval`, `fileLog`, `api`, `sourceRefs`).
- Validation feedback before save.
- Dry-run query test for source SQL mapping (`targetColumn`, `valueColumn`).
- Atomic write on save (temp file + rename) to avoid partial config state.

## 4.6 Web UI Delivery Decision

- V2 includes a built-in local Web UI as a supported delivery option.
- Default exposure is local-only (`127.0.0.1`) for safety.
- UI scope starts with configuration management/validation, then expands to monitoring dashboard.

MVP scope (ship target):

- Source CRUD UI.
- Server config edit UI.
- Validate + reload buttons.
- Basic action editor (`alert` / `script` / `notify`).

Post-MVP expansion:

- Monitoring dashboard pages.
- Notification channel templates.
- Role/auth integration (if remote exposure is required later).
- Change history/audit trail.

## 4.7 Monitoring Dashboard Delivery

- Dashboard can be provided in V2 as a local built-in page.
- First version should consume existing local APIs/log data (no extra external dependency).

Dashboard MVP widgets:

- Source status cards (latest value + alert/action state).
- Time-series chart per source/measure.
- Recent action/event timeline (`alert`/`script`/`notify`).
- Filter bar (`source`, `measure`, `time range`, `tag`).

Dashboard API options:

- Reuse existing `POST /status` and `POST /watch`.
- Optional optimized endpoints:
  - `GET /v2/dashboard/status`
  - `GET /v2/dashboard/series`
  - `GET /v2/dashboard/events`

Dashboard non-goals (initial):

- Multi-tenant auth.
- Remote internet exposure by default.
- Heavy analytics/long-term warehouse features.

## 5. Runtime Data Model (Internal)

- `QueryGroupRule`
  - Query metadata and result-shape parser.
- `TargetRule`
  - `target`, `measure`, `viewMode`, `tag`, `List<Rule>`.
- `Rule`
  - `id`, condition (`operator`, `threshold`), `List<Action>`.
- `Action`
  - Type, handler config, action name.
- `MetricPoint`
  - `source`, `measure`, `target`, `value`, `tag`, `actionStates`.

Index structures:

- `Map<String, TargetRule>` keyed by target.
- `Map<String, String>` for per-measure `viewMode` lookup.
- Optional cache for previous values keyed by `(source, measure)` for `viewMode=delta`.

## 6. Processing Flow

1. Load server config.
2. Resolve `sourceRefs[]` and load source config files.
3. Detect schema version and build merged rule graph.
4. Execute query.
5. Parse result rows:
   - `long`: read `targetColumn` + `valueColumn`.
   - `wide`: convert one row with many metric columns into multiple `(target, value)` events.
6. Fan-out each parsed event to `TargetRule`.
7. Apply `viewMode` transform (`raw` passthrough, `delta` previous-value subtraction).
8. Evaluate all `rules[]` under that target.
9. Dispatch matched rule `actions[]` by action type (`alert`/`script`/`notify`).
10. Build output `rc[]` items.
11. Persist log and action outputs.

## 7. Cut-over Policy

- V2 is a breaking change and does not preserve V1 runtime/config compatibility.
- Loader accepts only `schemaVersion: 2`.
- Legacy fields (`measure`, `alerts`, `alertScript`, `policy`, `value`) are unsupported in V2 runtime path.
- Deployment should switch to V2 split config (`server` + `source` files) before enabling V2 mode.

## 7.1 Migration Path (One-time)

- Phase 1: inventory current configs and map metrics/thresholds to `measureV2`/`rules`/`actions`.
- Phase 2: generate V2 split files (`server` + `source`) with a one-time conversion tool.
- Phase 3: validate with `/v2/config/validate` and dry-run queries.
- Phase 4: cut over to V2 runtime and remove legacy config files from active paths.

## 8. Implementation Plan (File-level)

## 8.1 Model / Config Parsing

- Add V2 model classes under `gop/src/main/java/model/`:
  - `MeasureV2.java`
  - `TargetV2.java`
  - `RuleV2.java`
  - `ActionV2.java`
- Extend config model:
  - `gop/src/main/java/model/SourceConfig.java`
  - `gop/src/main/java/model/Config.java`
- Add split-config models:
  - `gop/src/main/java/model/ServerConfigV2.java`
  - `gop/src/main/java/model/SourceFileV2.java`
- Add config resolver:
  - `gop/src/main/java/config/ConfigResolverV2.java`

## 8.2 Query Execution

- Refactor multi-result processing:
  - `gop/src/main/java/db/Database.java`
- Replace fixed positional assumptions with rule-based fan-out.

## 8.3 Value Transform / Action Evaluation

- Refactor `viewMode` transformation:
  - `gop/src/main/java/app/Gop.java` (value transform path and related call paths)
- Refactor action dispatch mapping:
  - `gop/src/main/java/log/FileLogService.java`
- Add action dispatcher + handler registry:
  - `gop/src/main/java/action/ActionDispatcher.java`
  - `gop/src/main/java/action/handler/AlertActionHandler.java`
  - `gop/src/main/java/action/handler/ScriptActionHandler.java`
  - `gop/src/main/java/action/handler/NotifyActionHandler.java`

## 8.4 Web/API Registration Layer

- Extend local API server:
  - `gop/src/main/java/api/ApiServer.java` (new `/v2/*` endpoints).
- Add config write service:
  - `gop/src/main/java/config/ConfigWriteServiceV2.java`
- Add schema/semantic validator service:
  - `gop/src/main/java/config/ConfigValidationServiceV2.java`
- Add static web UI (local admin page):
  - `gop/src/main/resources/web/v2/index.html`
  - `gop/src/main/resources/web/v2/app.js`
  - `gop/src/main/resources/web/v2/styles.css`

## 8.5 API / Watch Filtering

- Keep API contract unchanged.
- Validate `name`/`tag` filtering still works with V2-generated metric names:
  - `gop/src/main/java/api/ApiServer.java`
  - `gop/src/main/java/io/ReadLog.java`

## 8.6 Dashboard UI/API Layer

- Add dashboard web pages:
  - `gop/src/main/resources/web/v2/dashboard/index.html`
  - `gop/src/main/resources/web/v2/dashboard/app.js`
  - `gop/src/main/resources/web/v2/dashboard/styles.css`
- Add dashboard API adapter (reuse `/status` + `/watch`, optional `/v2/dashboard/*`):
  - `gop/src/main/java/api/ApiServer.java`
- Add DTO/aggregation helpers for chart-ready response:
  - `gop/src/main/java/api/dashboard/DashboardQueryService.java`
  - `gop/src/main/java/api/dashboard/DashboardDto.java`

## 8.7 Samples / Docs

- Add V2 default samples:
  - `conf/multi_target_alerts.json`
  - `conf/v2/server/default.json`
  - `conf/v2/sources/*.json`
- Update docs:
  - `README.md`
  - `docs/source/config.rst`
  - `docs/source/spec.rst`
  - `docs/config.schema.json`

## 9. Test Plan

## 9.1 Unit Tests

- Parser tests for `schemaVersion` branching.
- Server/source split parser tests (`sourceRefs` resolution).
- `long` shape parsing tests.
- `wide` shape fan-out tests.
- Multi-rule evaluation order and correctness.
- Multi-action dispatch under a matched rule.
- `viewMode` behavior (`raw`/`delta`) per target/measure key.
- Script action execution tests (`type=script`).
- Notify action payload rendering tests (`type=notify`).
- Config writer atomic-save tests.
- Validation service tests (invalid sourceRefs, duplicate source id, missing target mapping).
- Dashboard query aggregation tests (status/series/events).

## 9.2 Integration Tests

- Single query returning multiple targets.
- Multiple rules triggered for one target.
- Multiple actions dispatched from one matched rule.
- Split config load (`server file + multiple source files`) end-to-end.
- API `/status` and `/watch` filtering with V2-produced logs.
- `/v2/sources` and `/v2/server-config` CRUD flow end-to-end.
- `/v2/config/validate` and `/v2/config/reload` behavior tests.
- Dashboard endpoint/UI data flow (`status`, `series`, `events`) end-to-end.

## 9.3 Regression

- Existing CLI commands (`run`, `server`, `watch`, `ls`) unchanged.
- Web registration changes do not break existing CLI-only operation.
- Dashboard feature disabled/unused does not affect collection pipeline.
- Legacy config input fails fast with clear actionable validation errors.

## 10. Rollout Milestones

- M1: Config parser + model classes.
- M2: Split config resolver (`server + sourceRefs`) + one-time import helper.
- M3: Database fan-out + `viewMode` transform refactor.
- M4: Action dispatcher + `alert`/`script`/`notify` handlers.
- M5: Web registration UI + `/v2/*` config APIs.
- M6: Monitoring dashboard UI + dashboard APIs.
- M7: API/watch regression, docs, release notes.

## 11. Done Criteria

- V2 sample config runs on V2 runtime path.
- Split config mode (`server + source`) runs as primary path.
- One query can emit multiple metrics in a single cycle.
- Multiple actions per target are evaluated and dispatched correctly.
- Existing script action flow works via `type=script` without regression.
- New notify channel can be added without DB/query pipeline change.
- Unit/integration test suite passes.
- Source/server configs can be created/updated from local web page and applied safely.
- Dashboard shows latest status, trends, and recent actions using local APIs.
- Legacy config files are rejected with clear migration guidance.
