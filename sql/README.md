# JAEDAERO 데이터베이스 SQL 운영 기준

## 1. 신규 환경 구축

`jaedaero_db_v2.sql`은 최신 스키마를 한 번에 생성하는 기준 파일입니다. 기존 테이블을 모두 삭제하므로 신규 DB 또는 전체 초기화가 확정된 환경에서만 실행합니다.

```bash
mysql -u <DB_USER> -p <DB_NAME> < jaedaero_db_v2.sql
```

v2에는 2026-08-13까지의 스키마 마이그레이션 결과가 모두 반영되어 있습니다. v2를 실행한 신규 DB에는 `sql/migration` 파일을 다시 실행하지 않습니다.

## 2. 운영 기준 데이터

스키마 생성 후 다음 순서로 적용합니다. 세 파일은 재실행할 수 있으며 기존 사용자 이력을 삭제하지 않습니다.

1. `sql/seed/military_pay_policy.sql` — 군종·계급별 봉급 정책
2. `sql/seed/badge_policy.sql` — 성향별 뱃지 등급 정책
3. `sql/seed/challenge_mission_mock_data.sql` — 현재 운영 미션 6개

`challenge_mission_mock_data.sql`은 기존 경로 호환성을 위해 파일명을 유지하지만, 현재는 목데이터가 아니라 노션 기준 운영 미션 마스터입니다.

## 3. 선택적 테스트 데이터

아래 파일은 개발·QA 환경에서만 실행합니다. 운영 DB에는 기본 적용하지 않습니다.

- `sql/seed/ai_coach_mock_data.sql`
- `sql/seed/cohort_test_users.sql`

## 4. 기존 DB 업그레이드

운영 중인 DB에는 `jaedaero_db_v2.sql`을 실행하지 않습니다. 먼저 백업한 뒤 아직 적용하지 않은 `sql/migration` 파일만 날짜순으로 실행합니다.

- `ALTER TABLE` 기반 파일은 대부분 1회 실행 전용입니다.
- `20260811_backfill_user_badges.sql`은 스키마 변경이 아닌 기존 데이터 보정용입니다.
- `20260813_add_mission_code.sql`과 `20260813_expand_leave_mode_event.sql`까지 v2 기준 스키마에 반영되어 있습니다.
- `20260813_add_mission_code.sql`은 미션 노출 순서가 바뀌어도 기존 완료 이력의 의미가 유지되도록 영구 식별 코드를 추가합니다.
- `20260813_expand_leave_mode_event.sql`은 재실행 가능하며, 기존 일정의 이벤트명이 비어 있으면 `휴가 일정`으로 보정합니다.

## 5. 파일 역할

| 구분 | 역할 | 재실행 |
| --- | --- | --- |
| `jaedaero_db_v2.sql` | 신규 DB 기준 스키마 | 전체 초기화할 때만 |
| `sql/migration/*.sql` | 기존 DB의 증분 변경 이력 | 파일별 1회 |
| 운영 기준 시드 | 정책·미션 마스터 동기화 | 가능 |
| `*_mock_data.sql`, 테스트 사용자 시드 | 개발·QA 데이터 구성 | 해당 파일 설명 확인 |

## 6. 로컬 Gradle 작업

- `./gradlew dbMigrate -PconfirmLocalDb=<DB명>` — 확인한 로컬 DB의 기존 데이터를 삭제하고 v2 스키마만 재생성
- `./gradlew dbSeed` — 운영 기준 데이터만 적재
- `./gradlew dbSeedMock -PconfirmLocalDb=<DB명>` — 운영 기준 데이터 적용 후 개발·QA 목데이터 적재
- `./gradlew dbReset -PconfirmLocalDb=<DB명>` — v2 스키마 재생성 후 운영 기준 데이터 적재
- `./gradlew dbResetWithMock -PconfirmLocalDb=<DB명>` — v2 스키마와 운영 기준 데이터, 목데이터까지 모두 적재

`dbMigrate`와 `dbSeedMock`은 Railway 같은 원격 호스트에서는 실행을 거부하며, 로컬에서도 확인한 DB명을 옵션으로 명시해야 합니다. 운영 반영은 백업과 적용 대상 확인 후 별도 절차로 수행합니다.
