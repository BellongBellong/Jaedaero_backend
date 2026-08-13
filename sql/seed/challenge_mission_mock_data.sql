-- 챌린지 화면에서 현재 연결되었거나 후보로 선정된 미션 마스터 Mock 데이터입니다.
-- 실행: mysql -u <DB_USER> -p jaedaero < sql/seed/challenge_mission_mock_data.sql
-- 후보 미션의 신규 action_type은 노출 허용 목록과 프론트 라우팅을 추가한 뒤 화면에 연결합니다.

SET NAMES utf8mb4;
START TRANSACTION;

INSERT INTO mission (
    mission_type,
    mission_category,
    title,
    description,
    action_type,
    display_order,
    trigger_type,
    trigger_value,
    event_priority,
    is_active
)
SELECT
    mock_mission.mission_type,
    mock_mission.mission_category,
    mock_mission.title,
    mock_mission.description,
    mock_mission.action_type,
    mock_mission.display_order,
    mock_mission.trigger_type,
    mock_mission.trigger_value,
    mock_mission.event_priority,
    TRUE
FROM (
    SELECT
        NULL AS mission_type,
        'DAILY' AS mission_category,
        '오늘의 시장 리포트 보기' AS title,
        '오늘의 AI 시장 리포트를 확인해보세요.' AS description,
        'VIEW_MARKET_REPORT' AS action_type,
        1 AS display_order,
        'NONE' AS trigger_type,
        NULL AS trigger_value,
        0 AS event_priority
    UNION ALL SELECT NULL, 'DAILY', '오늘의 거래 내역 확인하기', '오늘 발생한 거래 내역을 확인해보세요.', 'VIEW_TRANSACTION_HISTORY', 2, 'NONE', NULL, 0
    UNION ALL SELECT NULL, 'DAILY', '오늘의 AI 리포트 보기', 'AI가 분석한 오늘의 리포트를 확인해보세요.', 'VIEW_AI_REPORT', 3, 'NONE', NULL, 0
    UNION ALL SELECT NULL, 'DAILY', '이번 달 소비 현황 보기', '이번 달 소비 현황을 확인해보세요.', 'VIEW_SPENDING_ANALYSIS', 4, 'NONE', NULL, 0
    UNION ALL SELECT NULL, 'DAILY', '나의 자산 현황 확인하기', '현재 보유 자산 현황을 확인해보세요.', 'VIEW_ASSET_STATUS', 5, 'NONE', NULL, 0
    UNION ALL SELECT 'SAFE', 'RECOMMENDED', '예금상품 살펴보기', '나에게 맞는 예금 상품을 확인해보세요.', 'VIEW_DEPOSIT_PRODUCT', 1, 'NONE', NULL, 0
    UNION ALL SELECT 'AGGRESSIVE', 'RECOMMENDED', '투자 추천 확인하기', 'AI가 제안한 투자 추천 내용을 확인해보세요.', 'VIEW_REBALANCING', 1, 'NONE', NULL, 0
    UNION ALL SELECT 'SAFE', 'ONE_TIME', 'What-if 시뮬레이션 하기', '시뮬레이션으로 자산 변화 목표를 설정해보세요.', 'RUN_WHAT_IF_SIMULATION', 1, 'NONE', NULL, 0
    UNION ALL SELECT NULL, 'EVENT', '월급날 자산 배분 해보기', '월급을 저축·투자·소비 목표에 맞춰 배분해보세요.', 'RUN_WHAT_IF_SIMULATION', 1, 'PAYDAY', 10, 1

    -- Swagger 기능 기반 후보 미션
    UNION ALL SELECT 'AGGRESSIVE', 'RECOMMENDED', '최신 투자 가이드 확인하기', '최신 투자 가이드를 확인해보세요.', 'VIEW_REBALANCING', 2, 'NONE', NULL, 0
    UNION ALL SELECT 'SAFE', 'RECOMMENDED', '추천 금융상품 비교하기', '나에게 맞는 추천 금융상품을 비교해보세요.', 'VIEW_DEPOSIT_PRODUCT', 2, 'NONE', NULL, 0
    UNION ALL SELECT 'SAFE', 'RECOMMENDED', '군인적금 상품 확인하기', '군인적금 상품과 혜택을 확인해보세요.', 'VIEW_SOLDIER_SAVINGS', 3, 'NONE', NULL, 0
    UNION ALL SELECT 'SAFE', 'ONE_TIME', '투자 목표 금액 설정하기', '투자 목표 금액을 설정해보세요.', 'UPDATE_GOAL', 2, 'NONE', NULL, 0
    UNION ALL SELECT 'AGGRESSIVE', 'ONE_TIME', '자동 투자 계획 저장하기', '자동 투자 계획을 저장해보세요.', 'SAVE_RECURRING_PLAN', 1, 'NONE', NULL, 0
    UNION ALL SELECT NULL, 'DAILY', '연결 계좌 현황 확인하기', '연결된 계좌 현황을 확인해보세요.', 'VIEW_ASSET_STATUS', 6, 'NONE', NULL, 0
    UNION ALL SELECT NULL, 'DAILY', 'ETF 시장 개요 살펴보기', 'ETF 시장의 주요 흐름을 확인해보세요.', 'VIEW_ETF_MARKET', 7, 'NONE', NULL, 0
    UNION ALL SELECT NULL, 'DAILY', 'ETF 거래량 상위 확인하기', '오늘의 ETF 거래량 상위 종목을 확인해보세요.', 'VIEW_ETF_TRADING', 8, 'NONE', NULL, 0
    UNION ALL SELECT 'SAFE', 'ONE_TIME', '전역 리포트 확인하기', '전역 후 재정 계획 리포트를 확인해보세요.', 'VIEW_DISCHARGE_REPORT', 3, 'NONE', NULL, 0
    UNION ALL SELECT 'SAFE', 'ONE_TIME', 'AI 소비 분석 실행하기', 'AI 소비 분석으로 지출 패턴을 확인해보세요.', 'RUN_AI_ANALYSIS', 4, 'NONE', NULL, 0
) AS mock_mission;

COMMIT;
