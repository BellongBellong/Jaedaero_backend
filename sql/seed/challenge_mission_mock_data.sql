-- 챌린지 미션 마스터 화면 확인용 Mock 데이터
-- 실행: mysql -u <DB_USER> -p jaedaero < sql/seed/challenge_mission_mock_data.sql
-- 제목이 같은 미션은 추가하지 않아 여러 번 실행해도 중복되지 않습니다.

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
    SELECT NULL AS mission_type, 'DAILY' AS mission_category, '데일리 금융 리포트 보기' AS title, '오늘의 금융 리포트를 확인해보세요.' AS description, 'VIEW_FINANCE_REPORT' AS action_type, 1 AS display_order, 'NONE' AS trigger_type, NULL AS trigger_value, 0 AS event_priority
    UNION ALL SELECT NULL, 'DAILY', '오늘의 거래 내역 확인하기', '오늘 발생한 거래 내역을 확인해보세요.', 'VIEW_TRANSACTION_HISTORY', 2, 'NONE', NULL, 0
    UNION ALL SELECT NULL, 'DAILY', '오늘의 AI 리포트 보기', 'AI가 분석한 오늘의 리포트를 확인해보세요.', 'VIEW_AI_REPORT', 3, 'NONE', NULL, 0
    UNION ALL SELECT NULL, 'DAILY', '데일리 시장 리포트 보기', '오늘의 시장 흐름을 확인해보세요.', 'VIEW_MARKET_REPORT', 4, 'NONE', NULL, 0
    UNION ALL SELECT NULL, 'DAILY', '이번 달 소비 현황 보기', '이번 달 소비 현황을 확인해보세요.', 'VIEW_SPENDING_ANALYSIS', 5, 'NONE', NULL, 0
    UNION ALL SELECT NULL, 'DAILY', '나의 자산 현황 확인하기', '현재 보유 자산 현황을 확인해보세요.', 'VIEW_ASSET_STATUS', 6, 'NONE', NULL, 0
    UNION ALL SELECT NULL, 'DAILY', '이번 주 지출 요약 보기', '이번 주 지출 요약을 확인해보세요.', 'VIEW_WEEKLY_SPENDING', 7, 'NONE', NULL, 0
    UNION ALL SELECT NULL, 'DAILY', '적금 납입 현황 확인하기', '적금 납입 현황을 확인해보세요.', 'VIEW_SAVING_STATUS', 8, 'NONE', NULL, 0
    UNION ALL SELECT NULL, 'DAILY', '목표 달성 현황 보기', '투자 목표의 진행도를 확인해보세요.', 'VIEW_GOAL_STATUS', 9, 'NONE', NULL, 0
    UNION ALL SELECT NULL, 'DAILY', '오늘의 금융 용어 보기', '오늘의 금융 용어를 알아보세요.', 'VIEW_FINANCE_CONTENT', 10, 'NONE', NULL, 0

    UNION ALL SELECT 'SAFE', 'ONE_TIME', 'What-if 시뮬레이션 하기', '시뮬레이션으로 자산 변화 목표를 설정해보세요.', 'RUN_WHAT_IF_SIMULATION', 1, 'NONE', NULL, 0
    UNION ALL SELECT 'AGGRESSIVE', 'ONE_TIME', '적립식 투자 목표 설정하기', '구체적인 투자 목표를 설정해보세요.', 'SET_INVESTMENT_GOAL', 2, 'NONE', NULL, 0
    UNION ALL SELECT NULL, 'ONE_TIME', '나의 투자 성향 확인하기', '나의 투자 성향 결과를 확인해보세요.', 'VIEW_INVESTMENT_PREFERENCE', 3, 'NONE', NULL, 0
    UNION ALL SELECT NULL, 'ONE_TIME', '첫 금융 리포트 보기', '금융 리포트를 처음 확인해보세요.', 'VIEW_FINANCE_REPORT', 4, 'NONE', NULL, 0
    UNION ALL SELECT NULL, 'ONE_TIME', '첫 시장 리포트 보기', '시장 리포트를 처음 확인해보세요.', 'VIEW_MARKET_REPORT', 5, 'NONE', NULL, 0
    UNION ALL SELECT 'SAFE', 'ONE_TIME', '첫 적금 목표 설정하기', '첫 적금 목표를 설정해보세요.', 'SET_SAVING_GOAL', 6, 'NONE', NULL, 0
    UNION ALL SELECT 'AGGRESSIVE', 'ONE_TIME', '첫 리밸런싱 제안 확인하기', '첫 리밸런싱 제안을 확인해보세요.', 'VIEW_REBALANCING', 7, 'NONE', NULL, 0

    UNION ALL SELECT 'SAFE', 'EVENT', '전역 100일 전 첫 휴가 일정 등록하기', '전역 전 휴가 일정을 등록해보세요.', 'CREATE_LEAVE_SCHEDULE', 1, 'DAYS_TO_DISCHARGE', 100, 1
    UNION ALL SELECT 'SAFE', 'EVENT', '휴가 예산 설정하기', '휴가 전에 사용할 예산을 설정해보세요.', 'SET_LEAVE_BUDGET', 2, 'LEAVE_SCHEDULED', NULL, 2
    UNION ALL SELECT NULL, 'EVENT', '월급날 월급 자산 배분하기', '이번 달 월급의 자산 배분을 확인해보세요.', 'VIEW_ASSET_ALLOCATION', 3, 'PAYDAY', 10, 3
    UNION ALL SELECT 'SAFE', 'EVENT', '전역 후 목표 금액 설정하기', '전역 후 필요한 목표 금액을 설정해보세요.', 'SET_GOAL_AMOUNT', 4, 'DAYS_TO_DISCHARGE', 30, 4
    UNION ALL SELECT 'AGGRESSIVE', 'EVENT', '전역 후 투자 계획 세우기', '전역 후 투자 목표를 설정해보세요.', 'SET_INVESTMENT_GOAL', 5, 'DAYS_TO_DISCHARGE', 30, 5
    UNION ALL SELECT 'SAFE', 'EVENT', '월급날 저축 금액 점검하기', '월급날 저축 목표를 확인해보세요.', 'VIEW_SAVING_STATUS', 6, 'PAYDAY', 10, 6
    UNION ALL SELECT 'AGGRESSIVE', 'EVENT', '월급날 투자 비중 점검하기', '월급날 투자 비중을 확인해보세요.', 'VIEW_ASSET_ALLOCATION', 7, 'PAYDAY', 10, 7
    UNION ALL SELECT NULL, 'EVENT', '휴가 전 소비 예산 점검하기', '휴가 전에 소비 예산을 점검해보세요.', 'VIEW_LEAVE_BUDGET', 8, 'LEAVE_SCHEDULED', NULL, 8

    UNION ALL SELECT 'SAFE', 'RECOMMENDED', '예금상품 살펴보기', '나에게 맞는 예금 상품을 확인해보세요.', 'VIEW_DEPOSIT_PRODUCT', 1, 'NONE', NULL, 0
    UNION ALL SELECT 'SAFE', 'RECOMMENDED', '적금상품 비교하기', '적금 상품의 금리와 조건을 비교해보세요.', 'VIEW_SAVING_PRODUCT', 2, 'NONE', NULL, 0
    UNION ALL SELECT 'SAFE', 'RECOMMENDED', '적금 투자 가이드 읽기', '적립식 투자 가이드를 확인해보세요.', 'VIEW_SAVING_GUIDE', 3, 'NONE', NULL, 0
    UNION ALL SELECT 'SAFE', 'RECOMMENDED', '나의 저축률 확인하기', '현재 저축률을 확인해보세요.', 'VIEW_SAVING_RATE', 4, 'NONE', NULL, 0
    UNION ALL SELECT 'SAFE', 'RECOMMENDED', '이번 달 예산 확인하기', '이번 달 예산 사용 현황을 확인해보세요.', 'VIEW_BUDGET_STATUS', 5, 'NONE', NULL, 0
    UNION ALL SELECT 'SAFE', 'RECOMMENDED', '지출 항목 점검하기', '지출이 많은 항목을 확인해보세요.', 'VIEW_SPENDING_ANALYSIS', 6, 'NONE', NULL, 0
    UNION ALL SELECT 'SAFE', 'RECOMMENDED', '목표 금액 다시 확인하기', '설정한 목표 금액을 확인해보세요.', 'VIEW_GOAL_STATUS', 7, 'NONE', NULL, 0
    UNION ALL SELECT 'SAFE', 'RECOMMENDED', '안전자산 비중 확인하기', '안전자산 비중을 확인해보세요.', 'VIEW_ASSET_ALLOCATION', 8, 'NONE', NULL, 0
    UNION ALL SELECT 'SAFE', 'RECOMMENDED', '비상금 목표 설정하기', '필요한 비상금 목표를 설정해보세요.', 'SET_EMERGENCY_FUND_GOAL', 9, 'NONE', NULL, 0
    UNION ALL SELECT 'SAFE', 'RECOMMENDED', '월 적금 목표 설정하기', '이번 달 적금 목표를 설정해보세요.', 'SET_SAVING_GOAL', 10, 'NONE', NULL, 0
    UNION ALL SELECT 'SAFE', 'RECOMMENDED', '소비 절약 팁 읽기', '절약에 도움 되는 콘텐츠를 확인해보세요.', 'VIEW_SAVING_TIP', 11, 'NONE', NULL, 0
    UNION ALL SELECT 'SAFE', 'RECOMMENDED', '고정지출 확인하기', '매달 반복되는 고정지출을 확인해보세요.', 'VIEW_FIXED_EXPENSE', 12, 'NONE', NULL, 0
    UNION ALL SELECT 'SAFE', 'RECOMMENDED', '다음 납입일 확인하기', '다음 적금 납입일을 확인해보세요.', 'VIEW_SAVING_SCHEDULE', 13, 'NONE', NULL, 0
    UNION ALL SELECT 'SAFE', 'RECOMMENDED', '저축 목표 진행도 보기', '저축 목표까지 남은 금액을 확인해보세요.', 'VIEW_GOAL_STATUS', 14, 'NONE', NULL, 0
    UNION ALL SELECT 'SAFE', 'RECOMMENDED', '안정형 포트폴리오 보기', '안정적인 자산 배분 예시를 확인해보세요.', 'VIEW_SAFE_PORTFOLIO', 15, 'NONE', NULL, 0

    UNION ALL SELECT 'AGGRESSIVE', 'RECOMMENDED', '리밸런싱 제안 확인하기', 'AI가 제안한 리밸런싱 내용을 확인해보세요.', 'VIEW_REBALANCING', 1, 'NONE', NULL, 0
    UNION ALL SELECT 'AGGRESSIVE', 'RECOMMENDED', '투자 상품 살펴보기', '관심 있는 투자 상품을 찾아보세요.', 'VIEW_INVESTMENT_PRODUCT', 2, 'NONE', NULL, 0
    UNION ALL SELECT 'AGGRESSIVE', 'RECOMMENDED', '공격형 포트폴리오 보기', '공격적인 자산 배분 예시를 확인해보세요.', 'VIEW_AGGRESSIVE_PORTFOLIO', 3, 'NONE', NULL, 0
    UNION ALL SELECT 'AGGRESSIVE', 'RECOMMENDED', 'AI 투자 분석 보기', 'AI 투자 분석 리포트를 확인해보세요.', 'VIEW_AI_INVESTMENT_ANALYSIS', 4, 'NONE', NULL, 0
    UNION ALL SELECT 'AGGRESSIVE', 'RECOMMENDED', '시장 변동성 확인하기', '현재 시장 변동성을 확인해보세요.', 'VIEW_MARKET_VOLATILITY', 5, 'NONE', NULL, 0
    UNION ALL SELECT 'AGGRESSIVE', 'RECOMMENDED', '수익률 현황 확인하기', '나의 투자 수익률을 확인해보세요.', 'VIEW_INVESTMENT_RETURN', 6, 'NONE', NULL, 0
    UNION ALL SELECT 'AGGRESSIVE', 'RECOMMENDED', '투자 비중 점검하기', '자산별 투자 비중을 확인해보세요.', 'VIEW_ASSET_ALLOCATION', 7, 'NONE', NULL, 0
    UNION ALL SELECT 'AGGRESSIVE', 'RECOMMENDED', '리스크 분석 확인하기', '현재 포트폴리오의 위험도를 확인해보세요.', 'VIEW_RISK_ANALYSIS', 8, 'NONE', NULL, 0
    UNION ALL SELECT 'AGGRESSIVE', 'RECOMMENDED', '관심 투자상품 저장하기', '관심 있는 투자 상품을 저장해보세요.', 'SAVE_INVESTMENT_PRODUCT', 9, 'NONE', NULL, 0
    UNION ALL SELECT 'AGGRESSIVE', 'RECOMMENDED', '투자 목표 수익률 설정하기', '투자 목표 수익률을 설정해보세요.', 'SET_TARGET_RETURN', 10, 'NONE', NULL, 0
    UNION ALL SELECT 'AGGRESSIVE', 'RECOMMENDED', '투자 가이드 읽기', '투자 전략 가이드를 확인해보세요.', 'VIEW_INVESTMENT_GUIDE', 11, 'NONE', NULL, 0
    UNION ALL SELECT 'AGGRESSIVE', 'RECOMMENDED', '시장 뉴스 요약 보기', '오늘의 시장 뉴스를 확인해보세요.', 'VIEW_MARKET_NEWS', 12, 'NONE', NULL, 0
    UNION ALL SELECT 'AGGRESSIVE', 'RECOMMENDED', '투자 시뮬레이션 해보기', '투자 시뮬레이션을 실행해보세요.', 'RUN_INVESTMENT_SIMULATION', 13, 'NONE', NULL, 0
    UNION ALL SELECT 'AGGRESSIVE', 'RECOMMENDED', '자산 배분 제안 보기', 'AI 자산 배분 제안을 확인해보세요.', 'VIEW_ASSET_ALLOCATION', 14, 'NONE', NULL, 0
    UNION ALL SELECT 'AGGRESSIVE', 'RECOMMENDED', '투자 성향 결과 다시 보기', '현재 투자 성향 결과를 확인해보세요.', 'VIEW_INVESTMENT_PREFERENCE', 15, 'NONE', NULL, 0
) AS mock_mission
WHERE NOT EXISTS (
    SELECT 1
    FROM mission existing_mission
    WHERE existing_mission.title = mock_mission.title
);

COMMIT;
