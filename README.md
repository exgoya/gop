# gop

gop는 Database 모니터링을 위한 Console 도구입니다.

- Query 추이
- System 자원 사용량
- 임계치 알람(수집 스크립트 발동)

## 설치 (쉬운 설치)

### Linux 패키지 (DEB / RPM)

1) 릴리즈에서 패키지 파일 다운로드
```
# 예시 파일명
# gop_1.0.1_amd64.deb
# gop-1.0.1-1.x86_64.rpm
```

2) 설치
```
# DEB
sudo dpkg -i gop_1.0.1_amd64.deb
sudo apt -f install

# RPM
sudo rpm -ivh gop-1.0.1-1.x86_64.rpm
```

3) 확인
```
gop version
```

> 패키지는 jpackage 기반이며 런타임을 포함합니다. (JDK 없이 실행 가능)

### APT/YUM 저장소 (GitHub Pages)

**APT (Debian/Ubuntu)**:
```
echo "deb [trusted=yes] https://exgoya.github.io/gop/repo/deb ./" | sudo tee /etc/apt/sources.list.d/gop.list
sudo apt update
sudo apt install gop
```

**YUM/DNF (RHEL/Fedora)**:
```
cat <<'EOF' | sudo tee /etc/yum.repos.d/gop.repo
[gop]
name=gop
baseurl=https://exgoya.github.io/gop/repo/rpm/
enabled=1
gpgcheck=0
EOF

sudo yum install gop
```

## 빠른 시작 (MySQL 예시)

### 1) 테스트용 DB 실행 (Docker)
```
docker compose -f docker/docker-test.yml up -d
```

### 2) 모니터링 실행
```
# 패키지 설치 시 (Linux)
gop server -config /opt/gop/config/mysql.json

# 소스 실행 시
gop server -config conf/mysql.json
```

### 3) 콘솔 모드(run)
```
# 패키지 설치 시 (Linux)
gop run -config /opt/gop/config/mysql.json -interval 1

# 소스 실행 시
gop run -config conf/mysql.json -interval 1
```

### 4) 로그 조회(watch)
```
# 로그 목록 확인
gop ls
gop ls <config>
gop ls <config>/<source>/2024

gop watch -config <config> -source <sourceId> -head 5
gop watch -config <config>
gop watch -config <config> -source <sourceId> tail -follow
```

## 명령어

```
gop server -config <config file path> [options]
gop run -config <config file path> [options]
gop ls [<config>[/<source>[/YYYY[/MM]]]] [-path <log root>]
gop watch [-config <config name>] [-source <sourceId>] [options]

gop version
```

- `server`: 수집 + 로그 저장 + API
- `run`: 콘솔 출력 전용 (sar 스타일)
- `watch`: 저장된 로그 조회 (자동 탐색)
- `ls`: 로그 목록 탐색 (config/source/연/월)

watch/ls 기본 로그 경로는 `data/`이며, 필요 시 `-path` 또는 `GOP_LOG_PATH` 환경변수로 변경할 수 있습니다.
동일한 source가 여러 config에 있으면 watch는 가장 최신 로그가 있는 config를 사용합니다.
`-follow`를 사용하면 tail -f처럼 로그가 추가될 때마다 계속 출력합니다.
`-source`를 생략하면 해당 config의 모든 source를 순서대로 출력합니다.

## 로그 경로 규칙

logPath 기준으로 `config/연/월/source` 디렉터리를 생성합니다.
config 폴더명은 config 파일명(확장자 제외)입니다.

예:
```
data/mysql/2026/01/mysql-local/log_20260131.json
```

서버 실행 시 해당 경로에 `config.json`을 자동 복사합니다.
같은 config 이름인데 내용이 다르면 기존 폴더를 백업하고 새 폴더를 생성합니다.

## Config

정형 스키마 파일: `docs/config.schema.json`

샘플 config:
- 패키지 설치:
  - `/opt/gop/config/config.json` (기본, MySQL)
  - `/opt/gop/config/mysql.json`
  - `/opt/gop/config/mariadb.json`
  - `/opt/gop/config/postgres.json`
  - `/opt/gop/config/oracle.json`
  - `/opt/gop/config/multi.json`
- 소스 실행:
  - `conf/mysql.json`
  - `conf/mariadb.json`
  - `conf/postgres.json`
  - `conf/oracle.json`
  - `conf/multi.json`

macOS/Windows 위치:
- macOS: `/Applications/gop.app/Contents/app/config`
- Windows: `C:\Program Files\gop\config`

### setting
- `jdbcSource` (object, required)
  - `url` (string, required): JDBC URL prefix (driver별)
  - `dbName` (string, required): DB name/service
  - `driverClass` (string, required): JDBC driver class
  - `jdbcProperties` (array, required): `{ name, value }` 목록
- `source` (string, optional): source id (멀티 DB 구분)
- `timeInterval` (int, required): polling interval (ms)
- `consolePrint` (bool, required): 콘솔 출력 여부
- `pageSize` (int|string, required): 헤더 반복 간격
- `retention` (int|string, required): 로그 보관 정책
- `printCSV` (bool, required): CSV 출력 모드
- `fileLog` (object, required)
  - `enable` (bool, required)
  - `logPath` (string, required)
  - `maxBytes` (int, optional, default 52428800)
  - `maxBackups` (int, optional, default 5)
- `api` (object, optional): local query API
  - `enable` (bool, required)
  - `port` (int, optional, default 18080)
  - `threadPoolSize` (int, optional, default 4)
  - `logPath` (string, optional): API log file path

### measure[] (required)
- `name` (string, required): 측정 이름
- `sql` (string, required): 실행 SQL/명령
- `tag` (string, optional): 태그
- `diff` (bool, optional): 이전 값과 차분
- `alertValue` (int, optional)
- `alertPolicy` (int, optional): 0/1/2/3
- `alertSql` (string, optional)
- `sqlIsOs` (bool, optional): OS command 여부
- `alertScript` (string, optional)
- `alertScriptIsOs` (bool, optional)

### sources[] (optional, multi-source)
멀티 소스 모니터링 시 `measure` 대신 `sources`를 사용합니다.

```
{
  "setting": { ... },
  "sources": [
    {
      "source": "mysql-local",
      "jdbcSource": { ... },
      "measure": [ ... ]
    },
    {
      "source": "postgres-local",
      "jdbcSource": { ... },
      "measure": [ ... ]
    }
  ]
}
```

source id 규칙: `A-Za-z0-9._-`만 허용 (공백/특수문자 불가)

## Local query API

daemon 실행 시(기본) 로컬 HTTP API로 떠있는 프로세스에 쿼리를 보낼 수 있습니다.

설정 예시:
```
"api": { "enable": true, "port": 18080, "threadPoolSize": 4, "logPath": "data/api.log" }
```

요청 예시:
```
POST http://127.0.0.1:18080/watch
{"source":"mysql-local","timeFrom":"2026-01-31 00:00:00.000","timeTo":"2026-01-31 23:59:59.999"}
```

요청 필드:
- `source` (required)
- `timeFrom` / `timeTo` (optional, `yyyy-MM-dd HH:mm:ss.SSS`)
- `name` / `tag` (optional)
- `head` / `tail` (optional)

API는 로그 파일을 읽어 반환하며, DB에 직접 쿼리는 하지 않습니다.

## Status API

현재 상태(최신 로그)를 반환합니다. `source`를 생략하면 모든 source를 반환합니다.

요청 예시:
```
POST http://127.0.0.1:18080/status
{"source":"mysql-local","name":"threads_connected"}
```

요청 필드:
- `source` (optional)
- `name` / `tag` (optional)

## Docker test (DB)

테스트용 DB 컨테이너:
```
docker compose -f docker/docker-test.yml up -d
```

MySQL: `conf/mysql.json` (source) / `/opt/gop/config/mysql.json` (package)
MariaDB: `conf/mariadb.json` (source) / `/opt/gop/config/mariadb.json` (package)
PostgreSQL: `conf/postgres.json` (source) / `/opt/gop/config/postgres.json` (package)

종료:
```
docker compose -f docker/docker-test.yml down
```

통합 테스트(옵션):
```
./docker/docker-test.sh
```

`RUN_DOCKER_TESTS=1`일 때만 DB 통합 테스트가 수행됩니다.

## Daemon (Linux / macOS)

Linux (systemd):
```
sudo install -m 644 scripts/gop.service /etc/systemd/system/gop.service
sudo systemctl daemon-reload
sudo systemctl enable --now gop
```

macOS (launchd):
```
sudo install -m 644 scripts/com.exgoya.gop.plist /Library/LaunchDaemons/com.exgoya.gop.plist
sudo launchctl load -w /Library/LaunchDaemons/com.exgoya.gop.plist
```

경로는 환경에 맞게 `ExecStart`/`ProgramArguments`와 `config.json` 경로를 수정하세요.

## Database support (drivers)

- Goldilocks: `sunje.goldilocks.jdbc.GoldilocksDriver` (bundled in `gop/lib/goldilocks8.jar`)
- MySQL: `com.mysql.cj.jdbc.Driver` (Maven)
- MariaDB: `org.mariadb.jdbc.Driver` (Maven)
- PostgreSQL: `org.postgresql.Driver` (Maven)
- Oracle: `oracle.jdbc.OracleDriver` (**manual**; add `ojdbc` jar to `gop/lib/`)

## Version

```
gop version
gop -version
```

## 개발/빌드

### 빌드 환경

- JDK 21
- Gradle 9.3.1

```
./gradlew -version
```

### 소스 빌드

```
./gradlew build
```

### 패키징

fat-jar:
```
./gradlew shadowJar
```

shadow zip:
```
./gradlew shadowDistZip
```

native app-image (Linux/macOS):
```
./gradlew jpackageAppImage
```

installers:
```
./gradlew jpackageDeb
./gradlew jpackageRpm
./gradlew jpackageDmg
./gradlew jpackagePkg
./gradlew jpackageMsi
```

Note: installer builds must be run on their target OS.

### package 구조

- `app` : entrypoint (main)
- `api` : local HTTP API
- `config` : config 로딩/경로/백업
- `cli` : 커맨드 파서
- `db` : DB 접근
- `io` : OS/로그 입력
- `log` : 로그 저장/로테이션
- `run` : run 모드 출력

## Roadmap

- report 기능 추가 (요약/통계 리포트, 향후 릴리즈 예정)

## alert policy

- 0 : not use ( default )
- 1 : is greater then alertValue ( query result > alertValue )
- 2 : is less then alertValue ( query result < alertValue )
- 3 : equal to alertValue ( query result = alertValue )
