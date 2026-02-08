# gop V2 계획 (멀티 타겟 + 멀티 액션)

## 1. 목표

- 하나의 쿼리에서 여러 타겟을 반환할 수 있도록 지원한다.
- 타겟별로 여러 액션(`alert` 포함)을 지원한다.
- 알림/액션 평가를 명시적이고 추적 가능하게 만들면서 런타임 오버헤드를 최소화한다.

## 2. 현재 한계 (V1)

- 쿼리 결과를 `measure` 당 단일 숫자 값으로만 처리한다.
- 한 행에 여러 컬럼/타겟이 있는 결과를 1급 개념으로 다루지 못한다.
- 알림 모델이 measure당 단일 규칙(`alertPolicy`, `alertValue`)에 고정되어 있다.
- 런타임 일부 로직이 위치 기반 매핑(인덱스 기반)에 의존한다.

## 2.1 문제 -> 지원 방향 (SQL 예시 포함)

### Case A: 타겟별로 쿼리를 따로 실행해야 함 (쿼리 수 과다)

- 기존 문제:
  - `Threads_connected`, `Threads_running`, `Connections`를 수집하려면 V1에서 `measure` 3개와 SQL 3회 실행이 필요하다.
- V2 방향:
  - 하나의 쿼리에서 여러 타겟을 반환하고, 런타임이 `target` 기준으로 fan-out한다.

V1 (개별 쿼리):

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

V2 (단일 쿼리, long shape):

```sql
select variable_name as target,
       cast(variable_value as signed) as value
from performance_schema.global_status
where variable_name in ('Threads_connected', 'Threads_running', 'Connections');
```

### Case B: 한 행에 여러 타겟 컬럼이 있는 경우 (wide row)

- 기존 문제:
  - 한 행에 여러 지표 컬럼이 있을 때, V1은 각 필드를 독립 타겟/지표로 처리하기 어렵다.
- V2 방향:
  - `resultShape=wide`를 지원하여 컬럼 매핑 기반 fan-out을 수행한다.
  - 가능하면 SQL 단계에서 `long` 형태로 정규화하는 방식을 권장한다.

Wide 쿼리 예시:

```sql
select
  sum(case when variable_name = 'Threads_connected' then cast(variable_value as signed) else 0 end) as threads_connected,
  sum(case when variable_name = 'Threads_running' then cast(variable_value as signed) else 0 end) as threads_running
from performance_schema.global_status;
```

정규화된 long 쿼리 예시:

```sql
select 'threads_connected' as target, sum(case when variable_name = 'Threads_connected' then cast(variable_value as signed) else 0 end) as value
from performance_schema.global_status
union all
select 'threads_running' as target, sum(case when variable_name = 'Threads_running' then cast(variable_value as signed) else 0 end) as value
from performance_schema.global_status;
```

### Case C: 지표당 알림/액션 규칙이 1개뿐임

- 기존 문제:
  - V1은 measure당 알림 규칙이 1개다.
- V2 방향:
  - 타겟별로 여러 액션(`warn`, `critical`, `script`, `notify`)을 독립 조건/핸들러로 평가한다.

타겟 기반 쿼리 예시:

```sql
select variable_name as target,
       cast(variable_value as signed) as value
from performance_schema.global_status
where variable_name = 'Threads_connected';
```

설정상의 타겟 rule 조건 예시:

- `warn`: `operator=gt`, `threshold=80`
- `critical`: `operator=gt`, `threshold=120` + 선택적 script

## 3. V2 설계 원칙

- 외부 설정은 사람이 읽기 쉬운 배열 기반 구조를 유지한다.
- 내부 런타임은 빠른 조회를 위해 키 기반(Map) 인덱스를 사용한다.
- 액션 스코프는 기본적으로 상위 target을 상속하고, 필요 시에만 override한다.
- 처리 안정성을 위해 결과 shape는 `target`, `value` 정규형(long)을 우선한다.
- 기존 script 실행 기능을 예외가 아니라 정식 액션 타입으로 모델링한다.
- 신규 메시지 연동은 지표 수집 로직 변경 없이 액션 핸들러 확장으로 처리한다.
- 서버 런타임 설정과 소스 쿼리 설정을 파일 레벨에서 분리한다.

## 4. 설정 모델 (V2)

## 4.1 버전 관리

- 최상위에 `schemaVersion`을 추가한다.
- V2 로더는 `schemaVersion: 2`만 허용한다.

## 4.2 서버 / 소스 분리 구조

- 설정을 아래 두 종류로 분리한다.
  - `server config`: 런타임/프로세스 설정.
  - `source config`: 소스별 JDBC/쿼리/액션 정의.
- 단순 배포를 위해 V2 단일(monolithic) 설정은 선택적으로 허용한다.

Server config 책임:

- 실행 주기/출력: `timeInterval`, `consolePrint`, `pageSize`, `printCSV`.
- 저장/API: `fileLog`, `api`.
- 소스 바인딩: `sourceRefs[]` (source id 또는 파일 경로).

Source config 책임:

- 식별: `source`.
- 연결: `jdbcSource`.
- 수집 규칙: `measureV2[]`.

권장 디렉터리 레이아웃:

- `conf/v2/server/default.json`
- `conf/v2/sources/mysql-local.json`
- `conf/v2/sources/postgres-local.json`

Server config 예시:

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

Source config 예시:

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

각 `measureV2`는 하나의 쿼리와 fan-out 규칙을 정의한다.

- `name`: 쿼리 그룹 ID.
- `sql`: SQL 문자열.
- `resultShape`: `long`(권장) 또는 `wide`.
- `targetColumn`: `long` shape의 타겟 컬럼.
- `valueColumn`: `long` shape의 값 컬럼.
- `defaultTag`: 선택적 기본 태그.
- `targets[]`: 타겟별 규칙.

각 target:

- `target`: DB 결과의 타겟 키.
- `measure`: 출력 지표명.
- `viewMode`: 값 모드 (`raw` 또는 `delta`).
- `tag`: 선택적 override 태그.
- `rules[]`: 타겟별 조건 규칙.

각 rule:

- `id`: 규칙 ID.
- `operator`: 비교 연산자 (`gt`, `lt`, `eq`).
- `threshold`: 임계값.
- `actions[]`: 조건 충족 시 실행할 액션 목록.

각 action:

- `name`: 액션 ID.
- `type`: 액션 타입 (`alert`, `script`, `notify`).
- `script`: 선택적 스크립트 본문/명령.
- `scriptIsOs`: 스크립트 실행 모드.
- `message`: 알림용 템플릿 메시지.
- `channel`: 목적지 채널 식별자 (`slack`, `teams`, `email` 등).
- `endpoint`: webhook류 호출 URL.

예시 (`rules[]`):

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

참고:

- rule 조건(`operator`/`threshold`)은 target 값에 대해 1회 평가한다.
- 매칭된 rule의 `actions[]`는 모두 디스패치한다.
- V2 런타임 경로에서는 `rules[]`만 사용한다.

## 4.4 Action Handler 모델

- `type=alert`
  - 지표 출력의 alert/action 상태를 설정한다.
- `type=script`
  - 로컬 스크립트/명령을 실행한다. (기존 기능의 정식 승격)
- `type=notify`
  - provider 어댑터를 통해 메시지를 전송한다. (`slack`, `teams`, `email`, `webhook`)

핸들러 인터페이스 방향:

- `ActionHandler`가 `action.type`으로 디스패치한다.
- 채널/공급자별 코드는 notifier adapter로 격리한다.
- 신규 채널 추가 시 metric 수집 파이프라인은 변경하지 않는다.

## 4.5 웹 등록 (Source + Config)

- 로컬 웹 페이지에서 아래를 지원한다.
  - source config 등록/수정/삭제
  - server config 등록/수정 (runtime, fileLog, api, sourceRefs)
- 1차는 로컬 바인딩(`127.0.0.1`)만 허용한다.

최소 API 세트:

- `GET /v2/sources`
- `GET /v2/sources/{sourceId}`
- `POST /v2/sources` (create/update)
- `DELETE /v2/sources/{sourceId}`
- `GET /v2/server-config`
- `POST /v2/server-config` (create/update)
- `POST /v2/config/validate` (문법 + 스키마 + 의미 검증)
- `POST /v2/config/reload` (가능한 경우 프로세스 재시작 없이 반영)

웹 페이지 요구사항:

- source 등록 폼 (`source`, `jdbcSource`, `measureV2`, `rules`, `actions`)
- server 등록 폼 (`timeInterval`, `fileLog`, `api`, `sourceRefs`)
- 저장 전 유효성 피드백
- source SQL 매핑 dry-run 테스트 (`targetColumn`, `valueColumn`)
- 저장 시 원자적 쓰기(임시 파일 + rename)로 partial write 방지

## 4.6 Web UI 제공 결정

- V2는 내장 로컬 Web UI 제공을 공식 옵션으로 포함한다.
- 기본 노출 범위는 안전을 위해 로컬(`127.0.0.1`)로 제한한다.
- UI는 설정 관리/검증부터 시작하고 이후 모니터링 대시보드로 확장한다.

MVP 범위 (출시 목표):

- Source CRUD UI
- Server config 편집 UI
- Validate + Reload 버튼
- 기본 액션 편집기 (`alert` / `script` / `notify`)

Post-MVP 확장:

- 모니터링 대시보드 페이지
- 알림 채널 템플릿
- 권한/인증 연동 (향후 원격 노출 필요 시)
- 변경 이력/감사 로그

## 4.7 모니터링 대시보드 제공

- V2에서 로컬 내장 페이지로 대시보드를 제공할 수 있다.
- 1차 버전은 기존 로컬 API/로그 데이터를 활용한다. (외부 의존성 추가 없음)

대시보드 MVP 위젯:

- Source 상태 카드 (최신 값 + alert/action 상태)
- Source/Measure 시계열 차트
- 최근 액션 이벤트 타임라인 (`alert`/`script`/`notify`)
- 필터 바 (`source`, `measure`, `time range`, `tag`)

대시보드 API 옵션:

- 기존 `POST /status`, `POST /watch` 재사용
- 선택적 최적화 endpoint:
  - `GET /v2/dashboard/status`
  - `GET /v2/dashboard/series`
  - `GET /v2/dashboard/events`

초기 비목표(non-goals):

- 멀티 테넌트 인증
- 기본 원격 인터넷 노출
- 장기 보관/웨어하우스급 분석

## 5. 런타임 데이터 모델 (내부)

- `QueryGroupRule`
  - 쿼리 메타데이터와 result shape 파서
- `TargetRule`
  - `target`, `measure`, `viewMode`, `tag`, `List<Rule>`
- `Rule`
  - `id`, 조건(`operator`, `threshold`), `List<Action>`
- `Action`
  - 타입, 핸들러 설정, 액션명
- `MetricPoint`
  - `source`, `measure`, `target`, `value`, `tag`, `actionStates`

인덱스 구조:

- `target` 키 기반 `Map<String, TargetRule>`
- 지표별 `viewMode` 조회용 `Map<String, String>`
- `viewMode=delta` 계산용 `(source, measure)` 키 이전값 캐시(선택)

## 6. 처리 흐름

1. server config 로드
2. `sourceRefs[]` 해석 후 source config 파일 로드
3. schema version 판별 및 병합 rule graph 구성
4. 쿼리 실행
5. 결과 행 파싱
   - `long`: `targetColumn` + `valueColumn` 읽기
   - `wide`: 한 행의 다중 컬럼을 `(target, value)` 이벤트로 변환
6. 파싱 이벤트를 `TargetRule` 기준 fan-out
7. `viewMode` 변환 적용 (`raw`는 그대로, `delta`는 이전값 차감)
8. 타겟별 `rules[]` 전부 평가
9. 매칭된 rule의 `actions[]`를 액션 타입(`alert`/`script`/`notify`)별 디스패치
10. 출력 `rc[]` 구성
11. 로그/액션 결과 저장

## 7. 전환(Cut-over) 정책

- V2는 파괴적 변경이며 V1 런타임/설정 호환을 유지하지 않는다.
- 로더는 `schemaVersion: 2`만 허용한다.
- 레거시 필드(`measure`, `alerts`, `alertScript`, `policy`, `value`)는 V2 런타임 경로에서 비지원이다.
- V2 모드 활성화 전, 배포 설정을 분리 구조(`server` + `source` 파일)로 전환한다.

## 7.1 마이그레이션 경로 (1회성)

- Phase 1: 현재 설정 인벤토리를 수집하고 metric/임계값을 `measureV2`/`rules`/`actions`로 매핑한다.
- Phase 2: 1회성 변환 도구로 V2 분리 파일(`server` + `source`)을 생성한다.
- Phase 3: `/v2/config/validate`와 dry-run query로 검증한다.
- Phase 4: V2 런타임으로 cut-over 후 활성 경로에서 레거시 설정 파일을 제거한다.

## 8. 구현 계획 (파일 단위)

## 8.1 모델 / 파서

- V2 모델 클래스 추가 (`gop/src/main/java/model/`)
  - `MeasureV2.java`
  - `TargetV2.java`
  - `RuleV2.java`
  - `ActionV2.java`
- 기존 config 모델 확장
  - `gop/src/main/java/model/SourceConfig.java`
  - `gop/src/main/java/model/Config.java`
- 분리 설정 모델 추가
  - `gop/src/main/java/model/ServerConfigV2.java`
  - `gop/src/main/java/model/SourceFileV2.java`
- 설정 해석기 추가
  - `gop/src/main/java/config/ConfigResolverV2.java`

## 8.2 쿼리 실행

- 다중 결과 처리 리팩터링
  - `gop/src/main/java/db/Database.java`
- 위치 기반 가정 제거, 규칙 기반 fan-out으로 교체

## 8.3 값 변환 / Action 평가

- `viewMode` 변환 로직 리팩터링
  - `gop/src/main/java/app/Gop.java` (값 변환 경로 및 호출부)
- action 디스패치 매핑 리팩터링
  - `gop/src/main/java/log/FileLogService.java`
- action dispatcher + handler 레지스트리 추가
  - `gop/src/main/java/action/ActionDispatcher.java`
  - `gop/src/main/java/action/handler/AlertActionHandler.java`
  - `gop/src/main/java/action/handler/ScriptActionHandler.java`
  - `gop/src/main/java/action/handler/NotifyActionHandler.java`

## 8.4 Web/API 등록 계층

- 로컬 API 서버 확장
  - `gop/src/main/java/api/ApiServer.java` (`/v2/*` endpoint 추가)
- 설정 쓰기 서비스 추가
  - `gop/src/main/java/config/ConfigWriteServiceV2.java`
- 스키마/의미 검증 서비스 추가
  - `gop/src/main/java/config/ConfigValidationServiceV2.java`
- 정적 Web UI 추가 (로컬 관리자 페이지)
  - `gop/src/main/resources/web/v2/index.html`
  - `gop/src/main/resources/web/v2/app.js`
  - `gop/src/main/resources/web/v2/styles.css`

## 8.5 API / Watch 필터링

- 기존 API 계약 유지
- V2 지표명에서도 `name`/`tag` 필터가 동작하는지 검증
  - `gop/src/main/java/api/ApiServer.java`
  - `gop/src/main/java/io/ReadLog.java`

## 8.6 대시보드 UI/API 계층

- 대시보드 웹 페이지 추가
  - `gop/src/main/resources/web/v2/dashboard/index.html`
  - `gop/src/main/resources/web/v2/dashboard/app.js`
  - `gop/src/main/resources/web/v2/dashboard/styles.css`
- 대시보드 API 어댑터 추가 (`/status` + `/watch` 재사용, 선택적 `/v2/dashboard/*`)
  - `gop/src/main/java/api/ApiServer.java`
- 차트 친화 DTO/집계 헬퍼 추가
  - `gop/src/main/java/api/dashboard/DashboardQueryService.java`
  - `gop/src/main/java/api/dashboard/DashboardDto.java`

## 8.7 샘플 / 문서

- V2 기본 샘플 추가
  - `conf/multi_target_alerts.json`
  - `conf/v2/server/default.json`
  - `conf/v2/sources/*.json`
- 문서 갱신
  - `README.md`
  - `docs/source/config.rst`
  - `docs/source/spec.rst`
  - `docs/config.schema.json`

## 9. 테스트 계획

## 9.1 단위 테스트

- `schemaVersion` 분기 파서 테스트
- server/source 분리 파서 테스트 (`sourceRefs` 해석)
- `long` shape 파싱 테스트
- `wide` shape fan-out 테스트
- 다중 rule 평가 순서/정확성 테스트
- 매칭된 rule 내 다중 action 디스패치 테스트
- target/measure 키 기반 `viewMode`(`raw`/`delta`) 테스트
- script action 실행 테스트 (`type=script`)
- notify action payload 렌더링 테스트 (`type=notify`)
- 설정 원자 저장(atomic save) 테스트
- 검증 서비스 테스트 (잘못된 sourceRefs, 중복 source id, 누락된 target 매핑)
- 대시보드 집계 질의 테스트 (status/series/events)

## 9.2 통합 테스트

- 하나의 쿼리에서 다중 타겟 반환
- 한 타겟에서 다중 rule 동시 트리거
- 매칭된 단일 rule에서 다중 action 동시 디스패치
- 분리 설정 로드 end-to-end (`server file + multiple source files`)
- V2 로그 기준 API `/status`, `/watch` 필터링 검증
- `/v2/sources`, `/v2/server-config` CRUD 플로우 end-to-end
- `/v2/config/validate`, `/v2/config/reload` 동작 검증
- 대시보드 endpoint/UI 데이터 플로우 (`status`, `series`, `events`) 검증

## 9.3 회귀 테스트

- 기존 CLI 명령(`run`, `server`, `watch`, `ls`) 동작 보장
- 웹 등록 기능이 CLI-only 운영을 깨지 않는지 검증
- 대시보드 미사용/비활성 시 수집 파이프라인 영향 없음
- 레거시 설정 입력 시 즉시 실패하고 명확한 전환 가이드를 제공하는지 검증

## 10. 롤아웃 마일스톤

- M1: Config 파서 + 모델 클래스
- M2: 분리 설정 해석기(`server + sourceRefs`) + 1회성 import 도구
- M3: DB fan-out + `viewMode` 변환 리팩터링
- M4: Action dispatcher + `alert`/`script`/`notify` 핸들러
- M5: 웹 등록 UI + `/v2/*` config API
- M6: 모니터링 대시보드 UI + dashboard API
- M7: API/watch 회귀, 문서, 릴리스 노트

## 11. 완료 기준

- V2 샘플 설정이 V2 런타임 경로에서 실행된다.
- 분리 설정 모드(`server + source`)가 기본 실행 경로로 동작한다.
- 하나의 쿼리에서 단일 사이클 내 다중 metric 생성이 가능하다.
- 타겟별 다중 action이 정확히 평가/디스패치된다.
- 기존 script 흐름이 `type=script`로 회귀 없이 동작한다.
- 신규 notify 채널을 DB/쿼리 파이프라인 변경 없이 확장할 수 있다.
- 단위/통합 테스트가 통과한다.
- 로컬 웹 페이지에서 source/server 설정을 안전하게 등록/수정/반영할 수 있다.
- 대시보드에서 최신 상태/추세/최근 액션을 로컬 API 기반으로 확인할 수 있다.
- 레거시 설정 파일은 명확한 마이그레이션 안내와 함께 거부된다.
