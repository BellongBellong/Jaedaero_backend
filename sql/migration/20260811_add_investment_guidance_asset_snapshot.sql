-- Issue #51: 투자 가이드 상세 화면에서 계산 시점의 안전·위험자산 현황을 표시한다.
-- 신규 설치는 jaedaero_db_v1.sql의 최종 정의를 사용한다.

ALTER TABLE investment_guidance
    ADD COLUMN safe_asset_amount BIGINT NOT NULL DEFAULT 0
        COMMENT '가이드 계산 당시 증권계좌 예수금(안전자산)' AFTER return_rate,
    ADD COLUMN risk_asset_amount BIGINT NOT NULL DEFAULT 0
        COMMENT '가이드 계산 당시 증권계좌 전체 보유종목 평가액(위험자산)' AFTER safe_asset_amount;
