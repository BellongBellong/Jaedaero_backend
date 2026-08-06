-- 캐시플로우용 병 봉급 정책 초기 데이터
-- 실행: mysql -u <DB_USER> -p jaedaero < sql/seed/military_pay_policy.sql
--
-- 현재 프로젝트의 2026 목업 기준값을 모든 군종에 공통 적용한다.
-- 연도별 실제 봉급이 확정되면 effective_year와 effective_from을 새로 추가한다.

START TRANSACTION;

INSERT IGNORE INTO military_pay_policy (
    soldier_type,
    rank_name,
    effective_year,
    monthly_salary,
    effective_from,
    effective_to
) VALUES
    ('ARMY', '이병', 2026, 200000, '2026-01-01', NULL),
    ('ARMY', '일병', 2026, 350000, '2026-01-01', NULL),
    ('ARMY', '상병', 2026, 650000, '2026-01-01', NULL),
    ('ARMY', '병장', 2026, 950000, '2026-01-01', NULL),
    ('NAVY', '이병', 2026, 200000, '2026-01-01', NULL),
    ('NAVY', '일병', 2026, 350000, '2026-01-01', NULL),
    ('NAVY', '상병', 2026, 650000, '2026-01-01', NULL),
    ('NAVY', '병장', 2026, 950000, '2026-01-01', NULL),
    ('AIRFORCE', '이병', 2026, 200000, '2026-01-01', NULL),
    ('AIRFORCE', '일병', 2026, 350000, '2026-01-01', NULL),
    ('AIRFORCE', '상병', 2026, 650000, '2026-01-01', NULL),
    ('AIRFORCE', '병장', 2026, 950000, '2026-01-01', NULL),
    ('MARINE', '이병', 2026, 200000, '2026-01-01', NULL),
    ('MARINE', '일병', 2026, 350000, '2026-01-01', NULL),
    ('MARINE', '상병', 2026, 650000, '2026-01-01', NULL),
    ('MARINE', '병장', 2026, 950000, '2026-01-01', NULL);

COMMIT;
