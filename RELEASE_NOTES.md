## v1.0.7 (2026-02-01)
- Full Changelog: https://github.com/exgoya/gop/compare/v1.0.6...v1.0.7

## v1.0.6 (2026-02-01)
- Full Changelog: https://github.com/exgoya/gop/compare/v1.0.5...v1.0.6

## v1.0.5 (2026-02-01)
- Full Changelog: https://github.com/exgoya/gop/compare/v1.0.4...v1.0.5

## v1.0.4 (2026-02-01)
- Full Changelog: https://github.com/exgoya/gop/compare/v1.0.3...v1.0.4

## Highlights
- 로그 탐색용 CLI 확장: `gop ls`, `gop watch` 개선
- `watch -follow`로 tail -f 스타일 스트리밍 지원
- 로그 저장 경로를 config 이름 기준으로 정리 + config snapshot/backup
- 문서 자동 버전 적용 및 가이드 정리

## Added
- `gop ls`로 config/source/연/월 탐색
- `gop watch -config <name>` 지원 (`-source`는 선택, 생략 시 전체 source 출력)
- `watch -follow`(tail -f) 지원
- 소스 실행용 샘플 config 디렉터리 `conf/` 추가
- docs 빌드시 최신 릴리즈 태그 기준으로 버전 자동 적용

## Changed
- 로그 저장 경로가 `<logPath>/<configName>/YYYY/MM/<source>/...`로 고정
- 문서/README에서 소스 실행 예시를 `conf/*.json` 기준으로 정리

## Notes
- 기존 로그는 이전 경로에 그대로 남음 (필요 시 `watch -f` 또는 폴더 이동)
