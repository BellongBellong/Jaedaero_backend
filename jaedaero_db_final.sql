-- ============================================================
-- JAEDAERO Database Final (schema through 2026-08-21, generated 2026-08-24)
-- Target: MySQL 8.4 / utf8mb4
--
-- 신규 환경 구축 또는 전체 초기화 전용 스크립트입니다.
-- 실행하면 아래 서비스 테이블과 데이터가 모두 삭제됩니다.
-- 기존 운영 DB의 증분 업그레이드에는 sql/migration만 사용합니다.
-- 스키마 생성 후 운영 기준 시드(봉급·뱃지·미션·휴가 혜택)를 순서대로 적용합니다.
-- 개발·QA 전용 사용자 및 목데이터 시드는 포함하지 않습니다.
-- ============================================================

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;
SET time_zone = '+09:00';

-- ---------------------------------------------
-- Drop existing tables in reverse dependency order
-- ---------------------------------------------
SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS `notification_outbox`;
DROP TABLE IF EXISTS `notification_campaign_receipt`;
DROP TABLE IF EXISTS `notification_campaign`;
DROP TABLE IF EXISTS `notification_history`;
DROP TABLE IF EXISTS `device_token`;
DROP TABLE IF EXISTS `daily_market_report_source`;
DROP TABLE IF EXISTS `daily_market_indicator`;
DROP TABLE IF EXISTS `daily_market_report`;
DROP TABLE IF EXISTS `leave_mode`;
DROP TABLE IF EXISTS `investment_badge`;
DROP TABLE IF EXISTS `user_badge`;
DROP TABLE IF EXISTS `badge`;
DROP TABLE IF EXISTS `user_mission_completion`;
DROP TABLE IF EXISTS `mission`;
DROP TABLE IF EXISTS `refresh_token`;
DROP TABLE IF EXISTS `discharge_report`;
DROP TABLE IF EXISTS `strategy_application`;
DROP TABLE IF EXISTS `investment_guidance`;
DROP TABLE IF EXISTS `recurring_investment_plan`;
DROP TABLE IF EXISTS `rebalancing_recommendation`;
DROP TABLE IF EXISTS `ai_recommended_scenario`;
DROP TABLE IF EXISTS `product_recommendation`;
DROP TABLE IF EXISTS `military_benefit`;
DROP TABLE IF EXISTS `financial_product`;
DROP TABLE IF EXISTS `ai_analysis`;
DROP TABLE IF EXISTS `simulation`;
DROP TABLE IF EXISTS `challenge_member_summary`;
DROP TABLE IF EXISTS `challenge_monthly_result`;
DROP TABLE IF EXISTS `challenge_member`;
DROP TABLE IF EXISTS `challenge_group`;
DROP TABLE IF EXISTS `asset_snapshot`;
DROP TABLE IF EXISTS `account_transaction_sync`;
DROP TABLE IF EXISTS `transaction_history`;
DROP TABLE IF EXISTS `soldier_saving`;
DROP TABLE IF EXISTS `connected_account`;
DROP TABLE IF EXISTS `codef_institution_connection`;
DROP TABLE IF EXISTS `codef_connection`;
DROP TABLE IF EXISTS `cashflow_forecast_month`;
DROP TABLE IF EXISTS `cashflow_forecast`;
DROP TABLE IF EXISTS `military_pay_policy`;
DROP TABLE IF EXISTS `goal`;
DROP TABLE IF EXISTS `user_agreement`;
DROP TABLE IF EXISTS `soldier_profile`;
DROP TABLE IF EXISTS `users`;
SET FOREIGN_KEY_CHECKS = 1;

-- ---------------------------------------------
-- 1. users : 사용자
-- ---------------------------------------------
CREATE TABLE users (
                       user_id      BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '사용자 ID',
                       social_type  ENUM('KAKAO', 'GOOGLE') NOT NULL COMMENT '소셜 로그인 유형',
                       social_id    VARCHAR(255) NOT NULL COMMENT '소셜 제공자 내 사용자 식별자',
                       nickname     VARCHAR(50) NULL COMMENT '닉네임',
                       profile_image  ENUM('ARMY', 'NAVY', 'AIRFORCE', 'MARINE') NOT NULL DEFAULT 'ARMY' COMMENT '프로필 아이콘. soldier_type과 같은 4종 값을 재사용하는 군종 스타일 아이콘',
                       profile_source ENUM('GREEN', 'OLIVE', 'YELLOW', 'ORANGE', 'GRAY', 'BLACK') NOT NULL DEFAULT 'GREEN' COMMENT '프로필 배경색. 6종 중 선택',
                       is_withdrawn BOOLEAN NOT NULL DEFAULT FALSE COMMENT '탈퇴 여부',
                       withdrawn_at TIMESTAMP NULL COMMENT '탈퇴 일시',
                       created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',
                       updated_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                           ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 일시',

                       CONSTRAINT uq_users_social_identity UNIQUE (social_type, social_id),
                       CONSTRAINT uq_users_nickname UNIQUE (nickname)
) COMMENT='사용자'
    DEFAULT CHARSET=utf8mb4
    COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------
-- 2. soldier_profile : 군 복무 프로필
-- ---------------------------------------------
CREATE TABLE soldier_profile (
                                 profile_id      BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '군 복무 프로필 ID',
                                 user_id         BIGINT NOT NULL COMMENT '사용자 ID',
                                 soldier_type    ENUM('ARMY', 'NAVY', 'AIRFORCE', 'MARINE') NOT NULL COMMENT '군종',
                                 rank_name       VARCHAR(20) NOT NULL COMMENT '현재 계급',
                                 enlistment_date DATE NOT NULL COMMENT '입대일',
                                 discharge_date  DATE NOT NULL COMMENT '전역 예정일',
                                 saving_join_yn  BOOLEAN NOT NULL DEFAULT FALSE COMMENT '장병내일준비적금 가입 여부',
                                 created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',
                                 updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                                     ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 일시',

                                 CONSTRAINT uq_soldier_profile_user UNIQUE (user_id),
                                 CONSTRAINT fk_soldier_profile_user
                                     FOREIGN KEY (user_id) REFERENCES users(user_id)
                                         ON DELETE CASCADE,
                                 CONSTRAINT chk_soldier_profile_date
                                     CHECK (discharge_date >= enlistment_date)
) COMMENT='군 복무 프로필'
    DEFAULT CHARSET=utf8mb4
    COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------
-- 3. user_agreement : 약관 동의 이력
-- ---------------------------------------------
CREATE TABLE user_agreement (
                                agreement_id      BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '약관 동의 ID',
                                user_id           BIGINT NOT NULL COMMENT '사용자 ID',
                                agreement_type    VARCHAR(50) NOT NULL COMMENT '약관 유형',
                                agreement_version VARCHAR(20) NOT NULL COMMENT '약관 버전',
                                is_required       BOOLEAN NOT NULL DEFAULT TRUE COMMENT '필수 약관 여부',
                                agreed_at         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '동의 일시',

                                CONSTRAINT uq_user_agreement UNIQUE (user_id, agreement_type, agreement_version),
                                CONSTRAINT fk_user_agreement_user
                                    FOREIGN KEY (user_id) REFERENCES users(user_id)
                                        ON DELETE CASCADE
) COMMENT='사용자 약관 동의'
    DEFAULT CHARSET=utf8mb4
    COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------
-- 4. goal : 전역 자산 목표
-- ---------------------------------------------
CREATE TABLE goal (
                      goal_id       BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '목표 ID',
                      user_id       BIGINT NOT NULL COMMENT '사용자 ID',
                      target_amount BIGINT NOT NULL DEFAULT 0 COMMENT '전역 자산 목표. 0은 목표 미설정 sentinel',
                      target_date   DATE NULL COMMENT '목표 달성 목표일',
                      status        ENUM('ACTIVE', 'COMPLETED', 'ARCHIVED') NOT NULL DEFAULT 'ACTIVE' COMMENT '목표 상태',
                      created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',
                      updated_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                          ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 일시',

                      CONSTRAINT uq_goal_user UNIQUE (user_id),
                      CONSTRAINT fk_goal_user
                          FOREIGN KEY (user_id) REFERENCES users(user_id)
                              ON DELETE CASCADE,
                      CONSTRAINT chk_goal_target_amount
                          CHECK (target_amount >= 0)
) COMMENT='전역 자산 목표'
    DEFAULT CHARSET=utf8mb4
    COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------
-- 5. military_pay_policy : 계급별 봉급 정책
-- ---------------------------------------------
CREATE TABLE military_pay_policy (
                                     pay_policy_id  BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '봉급 정책 ID',
                                     soldier_type   ENUM('ARMY', 'NAVY', 'AIRFORCE', 'MARINE') NOT NULL COMMENT '군종',
                                     rank_name      VARCHAR(20) NOT NULL COMMENT '계급',
                                     effective_year SMALLINT NOT NULL COMMENT '적용 연도',
                                     monthly_salary BIGINT NOT NULL COMMENT '월 봉급',
                                     effective_from DATE NOT NULL COMMENT '정책 시작일',
                                     effective_to   DATE NULL COMMENT '정책 종료일',
                                     created_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',

                                     CONSTRAINT uq_military_pay_policy
                                         UNIQUE (soldier_type, rank_name, effective_year),
                                     CONSTRAINT chk_military_pay_policy_salary
                                         CHECK (monthly_salary >= 0)
) COMMENT='계급별 봉급 정책 — cashflow_forecast.expected_salary 계산의 근거 데이터. 국방부 등 별도 소스 필요(CODEF 카탈로그 밖)'
    DEFAULT CHARSET=utf8mb4
    COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------
-- 6. cashflow_forecast : 캐시플로우 예측 요약
-- ---------------------------------------------
CREATE TABLE cashflow_forecast (
                                   forecast_id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '예측 결과 ID',
                                   user_id                  BIGINT NOT NULL COMMENT '사용자 ID',
                                   base_asset               BIGINT NOT NULL DEFAULT 0 COMMENT '계산 기준 현재 자산',
                                   expected_salary          BIGINT NOT NULL DEFAULT 0 COMMENT '전역까지 예상 총급여',
                                   expected_spending        BIGINT NOT NULL DEFAULT 0 COMMENT '전역까지 예상 총소비',
                                   expected_saving_amount   BIGINT NOT NULL DEFAULT 0 COMMENT '전역까지 예상 군적금 납입 원금',
                                   expected_investment_amount BIGINT NOT NULL DEFAULT 0 COMMENT '전역까지 예상 투자 원금',
                                   soldier_saving_principal BIGINT NOT NULL DEFAULT 0 COMMENT '군적금 총 원금(기존 잔액 + 전역까지 예상 신규 납입)',
                                   investment_principal     BIGINT NOT NULL DEFAULT 0 COMMENT '투자 총 원금(연동 증권계좌 매입금액 또는 백필 추정치 + 전역까지 예상 신규 납입)',
                                   expected_asset           BIGINT NOT NULL DEFAULT 0 COMMENT '전역 예상 자산(현재 순자산 + 급여-소비 + 군적금 이자·매칭지원금 + 투자 예상수익)',
                                   soldier_saving_interest  BIGINT NOT NULL DEFAULT 0 COMMENT '군적금 예상 이자 참고값',
                                   government_matching_support BIGINT NOT NULL DEFAULT 0 COMMENT '정부 매칭지원금 참고값',
                                   expected_investment_return BIGINT NOT NULL DEFAULT 0 COMMENT '예상 투자수익 참고값',
                                   projected_benefit_amount BIGINT NOT NULL DEFAULT 0 COMMENT '미확정 예상 혜택 합계',
                                   calculation_policy_version VARCHAR(50) NOT NULL DEFAULT 'CONSERVATIVE_CASHFLOW_V3_20260813' COMMENT '캐시플로우 계산 정책 버전',
                                   monthly_spending_limit   BIGINT NOT NULL DEFAULT 0 COMMENT '월 소비 상한선',
                                   achievement_rate         DECIMAL(5,2) NOT NULL DEFAULT 0 COMMENT '목표 달성률',
                                   financial_discharge_date DATE NULL COMMENT '재정적 전역일',
                                   policy_version           VARCHAR(30) NULL COMMENT '적용 봉급 정책 버전',
                                   generated_at             TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '계산 일시',

                                   CONSTRAINT fk_cashflow_forecast_user
                                       FOREIGN KEY (user_id) REFERENCES users(user_id)
                                           ON DELETE CASCADE,
                                   CONSTRAINT chk_cashflow_forecast_achievement_rate
                                       CHECK (achievement_rate BETWEEN 0 AND 999.99)
) COMMENT='캐시플로우 예측 요약'
    DEFAULT CHARSET=utf8mb4
    COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------
-- 7. cashflow_forecast_month : 월별 캐시플로우
-- ---------------------------------------------
CREATE TABLE cashflow_forecast_month (
                                         forecast_month_id        BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '월별 예측 ID',
                                         forecast_id               BIGINT NOT NULL COMMENT '예측 결과 ID',
                                         forecast_month             DATE NOT NULL COMMENT '예측 월의 첫날',
                                         expected_rank              VARCHAR(20) NULL COMMENT '예상 계급',
                                         expected_salary            BIGINT NOT NULL DEFAULT 0 COMMENT '예상 급여',
                                         expected_saving_amount     BIGINT NOT NULL DEFAULT 0 COMMENT '예상 저축액',
                                         expected_investment_amount BIGINT NOT NULL DEFAULT 0 COMMENT '예상 투자액',
                                         expected_spending_amount   BIGINT NOT NULL DEFAULT 0 COMMENT '예상 소비액',
                                         expected_ending_asset      BIGINT NOT NULL DEFAULT 0 COMMENT '월말 예상 자산',

                                         CONSTRAINT uq_cashflow_forecast_month
                                             UNIQUE (forecast_id, forecast_month),
                                         CONSTRAINT fk_cashflow_forecast_month_forecast
                                             FOREIGN KEY (forecast_id) REFERENCES cashflow_forecast(forecast_id)
                                                 ON DELETE CASCADE
) COMMENT='월별 캐시플로우 예측 — 홈 탭 "월별 자산 흐름 그래프"의 데이터 소스'
    DEFAULT CHARSET=utf8mb4
    COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------
-- 8. codef_connection : CODEF 금융 연동
-- ---------------------------------------------
CREATE TABLE codef_connection (
                                  connection_id            BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '금융 연동 ID',
                                  user_id                  BIGINT NOT NULL COMMENT '사용자 ID',
                                  connected_id_encrypted   VARCHAR(1024) NOT NULL COMMENT '암호화된 CODEF Connected ID',
                                  connected_id_hash        CHAR(64) NOT NULL COMMENT 'Connected ID SHA-256 해시',
                                  status                   ENUM('ACTIVE', 'DISCONNECTED', 'ERROR') NOT NULL DEFAULT 'ACTIVE' COMMENT '연동 상태',
                                  last_sync_at             TIMESTAMP NULL COMMENT '전체 계좌 마지막 동기화 시각(연동 단위)',
                                  last_sync_error_message  VARCHAR(500) NULL COMMENT '최근 동기화 실패 사유',
                                  created_at               TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',
                                  updated_at               TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                                      ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 일시',

                                  CONSTRAINT uq_codef_connection_user UNIQUE (user_id),
                                  CONSTRAINT uq_codef_connection_connected_hash UNIQUE (connected_id_hash),
                                  CONSTRAINT fk_codef_connection_user
                                      FOREIGN KEY (user_id) REFERENCES users(user_id)
                                          ON DELETE CASCADE
) COMMENT='CODEF 금융기관 연동'
    DEFAULT CHARSET=utf8mb4
    COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------
-- 9. codef_institution_connection : 기관별 CODEF 로그인 정보
-- ---------------------------------------------
CREATE TABLE codef_institution_connection (
                                              institution_connection_id   BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '기관별 연동 ID',
                                              connection_id               BIGINT NOT NULL COMMENT 'CODEF 금융 연동 ID',
                                              institution_code            VARCHAR(20) NOT NULL COMMENT 'CODEF organization 코드',
                                              business_type               ENUM('BK', 'ST') NOT NULL COMMENT 'CODEF 업무 구분(BK 은행, ST 증권)',
                                              login_type                  VARCHAR(10) NOT NULL COMMENT 'CODEF 로그인 방식(아이디/비밀번호는 1)',
                                              login_id_encrypted          VARCHAR(1024) NULL COMMENT '암호화된 기관 로그인 ID',
                                              login_password_encrypted    VARCHAR(1024) NOT NULL COMMENT '암호화된 기관 로그인 비밀번호',
                                              birth_date_encrypted        VARCHAR(1024) NULL COMMENT '암호화된 생년월일',
                                              status                      ENUM('ACTIVE', 'DISCONNECTED', 'ERROR') NOT NULL DEFAULT 'ACTIVE' COMMENT '기관 연동 상태',
                                              last_sync_at                TIMESTAMP NULL COMMENT '기관별 마지막 동기화 시각',
                                              last_sync_error_message     VARCHAR(500) NULL COMMENT '기관별 최근 동기화 실패 사유',
                                              created_at                  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',
                                              updated_at                  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                                                  ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 일시',

                                              CONSTRAINT uq_codef_institution_connection
                                                  UNIQUE (connection_id, institution_code, business_type),
                                              CONSTRAINT fk_codef_institution_connection_connection
                                                  FOREIGN KEY (connection_id) REFERENCES codef_connection(connection_id)
                                                      ON DELETE CASCADE,
                                              INDEX idx_codef_institution_connection_status (connection_id, status)
) COMMENT='CODEF 기관별 로그인 정보 및 동기화 상태. 로그인 식별자와 비밀번호는 평문 저장 금지'
    DEFAULT CHARSET=utf8mb4
    COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------
-- 10. connected_account : CODEF 연동 계좌
-- ---------------------------------------------
CREATE TABLE connected_account (
                                   account_id                BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '연동 계좌 ID',
                                   connection_id             BIGINT NOT NULL COMMENT 'CODEF 연동 ID',
                                   institution_code          VARCHAR(20) NOT NULL COMMENT 'CODEF organization 코드',
                                   institution_name          VARCHAR(100) NOT NULL COMMENT '금융기관명',
                                   account_number_encrypted  VARCHAR(1024) NOT NULL COMMENT '암호화된 실제 계좌번호',
                                   account_number_hash       CHAR(64) NOT NULL COMMENT '계좌번호 SHA-256 해시',
                                   account_masked            VARCHAR(50) NOT NULL COMMENT '화면 표시용 마스킹 계좌번호',
                                   business_type             ENUM('BK', 'ST') NOT NULL DEFAULT 'BK' COMMENT 'CODEF 업무 구분(BK 은행, ST 증권)',
                                   account_type              VARCHAR(50) NULL COMMENT '계좌유형',
                                   account_role              ENUM('SOLDIER_SAVING', 'NARASARANG', 'GENERAL') NOT NULL DEFAULT 'GENERAL' COMMENT '계좌 분류',
                                   product_name               VARCHAR(255) NULL COMMENT '상품명',
                                   current_balance           BIGINT NOT NULL DEFAULT 0 COMMENT '조회 시점 잔액',
                                   available_balance         BIGINT NULL COMMENT '출금 가능 금액',
                                   account_opened_date       DATE NULL COMMENT '계좌 개설일',
                                   maturity_date             DATE NULL COMMENT '만기일',
                                   last_synced_at            TIMESTAMP NULL COMMENT '계좌별 최근 조회일',
                                   last_processed_transaction_id BIGINT NULL COMMENT '배치 diff 커서',
                                   status                    ENUM('ACTIVE', 'DISCONNECTED') NOT NULL DEFAULT 'ACTIVE' COMMENT '계좌 상태',
                                   created_at                TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',
                                   updated_at                TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                                       ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 일시',

                                   CONSTRAINT uq_connected_account_source
                                       UNIQUE (connection_id, institution_code, account_number_hash),
                                   CONSTRAINT fk_connected_account_connection
                                       FOREIGN KEY (connection_id) REFERENCES codef_connection(connection_id)
                                           ON DELETE CASCADE
) COMMENT='CODEF 연동 계좌 — MVP는 수시입출·정기적금·증권 계좌만 후속 동기화 대상(신탁·외화·펀드·대출은 저장은 하되 거래내역 미수집)'
    DEFAULT CHARSET=utf8mb4
    COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------
-- 10. soldier_saving : 장병내일준비적금 상세
-- ---------------------------------------------
CREATE TABLE soldier_saving (
                                saving_id                    BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '장병 적금 ID',
                                user_id                      BIGINT NOT NULL COMMENT '사용자 ID',
                                account_id                   BIGINT NOT NULL COMMENT '연동 적금 계좌 ID',
                                bank_name                    VARCHAR(100) NULL COMMENT '은행명',
                                monthly_amount               BIGINT NULL COMMENT '월 납입액',
                                interest_rate                DECIMAL(5,2) NULL COMMENT '적용 금리',
                                government_support_expected  BIGINT NULL COMMENT '예상 정부 매칭지원금',
                                start_date                   DATE NULL COMMENT '가입일',
                                end_date                     DATE NULL COMMENT '만기일',
                                created_at                   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',
                                updated_at                   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                                    ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 일시',

                                CONSTRAINT uq_soldier_saving_account UNIQUE (account_id),
                                CONSTRAINT fk_soldier_saving_user
                                    FOREIGN KEY (user_id) REFERENCES users(user_id)
                                        ON DELETE CASCADE,
                                CONSTRAINT fk_soldier_saving_account
                                    FOREIGN KEY (account_id) REFERENCES connected_account(account_id)
                                        ON DELETE CASCADE
) COMMENT='장병내일준비적금 상세'
    DEFAULT CHARSET=utf8mb4
    COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------
-- 11. transaction_history : 거래 내역
-- ---------------------------------------------
CREATE TABLE transaction_history (
                                     transaction_id           BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '거래 내역 ID',
                                     account_id               BIGINT NOT NULL COMMENT '연동 계좌 ID',
                                     transaction_datetime     DATETIME NOT NULL COMMENT '거래 일시',
                                     amount                   BIGINT NOT NULL COMMENT '거래 금액(항상 0 이상)',
                                     balance_after             BIGINT NULL COMMENT '거래 후 잔액',
                                     transaction_type         ENUM('DEPOSIT', 'WITHDRAW') NOT NULL COMMENT '입금 또는 출금',
                                     category                 VARCHAR(50) NULL COMMENT 'AI 소비 카테고리',
                                     category_source          ENUM('RULE', 'AI', 'USER') NULL COMMENT '카테고리 생성 주체',
                                     transaction_description  VARCHAR(255) NULL COMMENT '거래 적요 또는 거래처',
                                     external_transaction_key CHAR(64) NOT NULL COMMENT '거래 중복 방지 SHA-256 해시',
                                     created_at               TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',

                                     CONSTRAINT uq_transaction_history_external_key
                                         UNIQUE (account_id, external_transaction_key),
                                     CONSTRAINT fk_transaction_history_account
                                         FOREIGN KEY (account_id) REFERENCES connected_account(account_id)
                                             ON DELETE CASCADE,
                                     CONSTRAINT chk_transaction_history_amount
                                         CHECK (amount >= 0)
) COMMENT='거래 내역'
    DEFAULT CHARSET=utf8mb4
    COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------
-- 12. account_transaction_sync : 계좌별 거래내역 동기화 범위
-- ---------------------------------------------
CREATE TABLE account_transaction_sync (
                                          transaction_sync_id   BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '거래내역 동기화 ID',
                                          account_id            BIGINT NOT NULL COMMENT '연동 계좌 ID',
                                          inquiry_type          ENUM('DEMAND_DEPOSIT', 'INSTALLMENT_SAVINGS') NOT NULL COMMENT '거래 조회 유형',
                                          requested_start_date  DATE NOT NULL COMMENT 'CODEF 조회 시작일',
                                          requested_end_date    DATE NOT NULL COMMENT 'CODEF 조회 종료일',
                                          synced_at             TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '동기화 일시',

                                          CONSTRAINT uq_account_transaction_sync_period
                                              UNIQUE (account_id, inquiry_type, requested_start_date, requested_end_date),
                                          CONSTRAINT fk_account_transaction_sync_account
                                              FOREIGN KEY (account_id) REFERENCES connected_account(account_id)
                                                  ON DELETE CASCADE,
                                          CONSTRAINT chk_account_transaction_sync_period
                                              CHECK (requested_start_date <= requested_end_date)
) COMMENT='계좌별 CODEF 거래내역 동기화 범위'
    DEFAULT CHARSET=utf8mb4
    COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------
-- 13. asset_snapshot : 일별 자산 스냅샷
-- ---------------------------------------------
CREATE TABLE asset_snapshot (
                                snapshot_id     BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '자산 스냅샷 ID',
                                user_id         BIGINT NOT NULL COMMENT '사용자 ID',
                                total_asset     BIGINT NOT NULL DEFAULT 0 COMMENT '총 자산',
                                total_saving    BIGINT NOT NULL DEFAULT 0 COMMENT '총 저축',
                                total_spending  BIGINT NOT NULL DEFAULT 0 COMMENT '총 소비',
                                snapshot_date   DATE NOT NULL COMMENT '기준일',
                                created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',

                                CONSTRAINT uq_asset_snapshot_user_date UNIQUE (user_id, snapshot_date),
                                CONSTRAINT fk_asset_snapshot_user
                                    FOREIGN KEY (user_id) REFERENCES users(user_id)
                                        ON DELETE CASCADE
) COMMENT='일별 자산 스냅샷 — connected_account.current_balance 합계 + transaction_history 집계로 계산하는 파생 데이터(CODEF 직접 매핑 아님)'
    DEFAULT CHARSET=utf8mb4
    COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------
-- 13. challenge_group : 입대 동기 챌린지 그룹
-- ---------------------------------------------
CREATE TABLE challenge_group (
                                 group_id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '동기 그룹 ID',
                                 soldier_type      ENUM('ARMY', 'NAVY', 'AIRFORCE', 'MARINE') NOT NULL COMMENT '군종',
                                 enlistment_year   INT NOT NULL COMMENT '입대 연도',
                                 enlistment_month  INT NOT NULL COMMENT '입대 월',
                                 created_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',

                                 CONSTRAINT uq_challenge_group_cohort
                                     UNIQUE (soldier_type, enlistment_year, enlistment_month),
                                 CONSTRAINT chk_challenge_group_month
                                     CHECK (enlistment_month BETWEEN 1 AND 12)
) COMMENT='입대 동기 챌린지 그룹'
    DEFAULT CHARSET=utf8mb4
    COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------
-- 14. challenge_member : 챌린지 참여자
-- ---------------------------------------------
CREATE TABLE challenge_member (
                                  member_id    BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '챌린지 참여 ID',
                                  group_id     BIGINT NOT NULL COMMENT '동기 그룹 ID',
                                  user_id      BIGINT NOT NULL COMMENT '사용자 ID',
                                  joined_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '참여 일시',

                                  INDEX idx_challenge_member_user_joined_at (user_id, joined_at DESC),
                                  CONSTRAINT uq_challenge_member_group_user UNIQUE (group_id, user_id),
                                  CONSTRAINT fk_challenge_member_group
                                      FOREIGN KEY (group_id) REFERENCES challenge_group(group_id)
                                          ON DELETE CASCADE,
                                  CONSTRAINT fk_challenge_member_user
                                      FOREIGN KEY (user_id) REFERENCES users(user_id)
                                          ON DELETE CASCADE
) COMMENT='챌린지 참여자 — 미션 완료 수와 순위는 챌린지 결과 테이블로 이력화(현재값 캐시 아님)'
    DEFAULT CHARSET=utf8mb4
    COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------
-- 15. challenge_monthly_result : 월별 챌린지 결과
-- ---------------------------------------------
CREATE TABLE challenge_monthly_result (
                                          challenge_result_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '월별 챌린지 결과 ID',
                                          member_id           BIGINT NOT NULL COMMENT '챌린지 참여 ID',
                                          result_month         DATE NOT NULL COMMENT '결과 월의 첫날',
                                          mission_completion_count INT NOT NULL DEFAULT 0 COMMENT '해당 월 미션 완료 수',
                                          created_at           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',

                                          CONSTRAINT uq_challenge_monthly_result
                                              UNIQUE (member_id, result_month),
                                          CONSTRAINT fk_challenge_monthly_result_member
                                              FOREIGN KEY (member_id) REFERENCES challenge_member(member_id)
                                                  ON DELETE CASCADE,
                                          CONSTRAINT chk_challenge_monthly_result_completion_count
                                              CHECK (mission_completion_count >= 0)
) COMMENT='월별 챌린지 결과 — 월별 미션 완료 수 집계'
    DEFAULT CHARSET=utf8mb4
    COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------
-- 16. challenge_member_summary : 챌린지 참여자 누적 현황
-- ---------------------------------------------
CREATE TABLE challenge_member_summary (
                                          member_id               BIGINT PRIMARY KEY COMMENT '챌린지 참여 ID',
                                          total_mission_count     INT NOT NULL DEFAULT 0 COMMENT '누적 미션 완료 수',
                                          updated_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                                              ON UPDATE CURRENT_TIMESTAMP COMMENT '집계 갱신 일시',

                                          CONSTRAINT fk_challenge_member_summary_member
                                              FOREIGN KEY (member_id) REFERENCES challenge_member(member_id)
                                                  ON DELETE CASCADE,
                                          CONSTRAINT chk_challenge_member_summary_mission_count
                                              CHECK (total_mission_count >= 0)
) COMMENT='챌린지 참여자의 누적 미션 완료 수 집계값'
    DEFAULT CHARSET=utf8mb4
    COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------
-- 17. simulation : 사용자 What-if 시뮬레이션
-- ---------------------------------------------
CREATE TABLE simulation (
                            simulation_id            BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '시뮬레이션 ID',
                            user_id                  BIGINT NOT NULL COMMENT '사용자 ID',
                            scenario_name             VARCHAR(100) NOT NULL COMMENT '시나리오명',
                            target_amount             BIGINT NOT NULL COMMENT '시나리오 평가 목표금액 스냅샷',
                            monthly_saving_amount    BIGINT NOT NULL COMMENT '장병내일준비적금 월 납입액(원, 0~550000)',
                            monthly_investment_amount BIGINT NOT NULL COMMENT '군적금 외 월 투자 배분액(원)',
                            expected_return_rate      DECIMAL(5,2) NOT NULL COMMENT '사용자 입력 목표 투자수익률(%, 연 환산 가정)',
                            monthly_spending_amount  BIGINT NOT NULL COMMENT '월 소비액(원)',
                            expected_asset            BIGINT NOT NULL COMMENT '전역 예상 자산(현재 순자산 + 급여-소비 + 군적금 이자·매칭지원금 + 투자 예상수익)',
                            financial_discharge_date DATE NULL COMMENT '이 시나리오 기준 재정적 전역일',
                            calculation_months        INT NULL COMMENT '상세 계산에 포함한 개월 수',
                            base_asset                BIGINT NULL COMMENT '계산 시점 현재 자산 스냅샷',
                            expected_salary           BIGINT NULL COMMENT '계산 기간 예상 급여 합계',
                            expected_spending         BIGINT NULL COMMENT '계산 기간 예상 소비 합계',
                            soldier_saving_principal  BIGINT NULL COMMENT '군적금 총 원금(기존 잔액 + 전역까지 예상 신규 납입)',
                            soldier_saving_interest   BIGINT NULL COMMENT '기존 잔액 경과기간 + 미래 회차 잔여기간의 연 5% 단리 예상 이자',
                            government_matching_support BIGINT NULL COMMENT '군적금 총 원금의 100% 매칭지원금 가정',
                            investment_principal      BIGINT NULL COMMENT '투자 총 원금(CODEF 매입원가 또는 백필 + 미래 납입)',
                            expected_investment_return BIGINT NULL COMMENT '기존 원금 + 미래 회차의 목표수익률 월복리 예상 투자수익',
                            unallocated_principal     BIGINT NULL COMMENT '급여에서 소비·군적금·투자 후 남는 금액 합계',
                            calculation_policy_version VARCHAR(50) NULL COMMENT '상세 계산 정책 버전',
                            is_saved                 BOOLEAN NOT NULL DEFAULT TRUE COMMENT '사용자 저장 여부',
                            created_at               TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',
                            updated_at               TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                                ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 일시',

                            CONSTRAINT fk_simulation_user
                                FOREIGN KEY (user_id) REFERENCES users(user_id)
                                    ON DELETE CASCADE,
                            CONSTRAINT chk_simulation_monthly_amounts
                                CHECK (
                                    monthly_spending_amount >= 0
                                        AND monthly_saving_amount BETWEEN 0 AND 550000
                                        AND monthly_investment_amount >= 0
                                    ),
                            CONSTRAINT chk_simulation_target_amount
                                CHECK (target_amount > 0),
                            CONSTRAINT chk_simulation_detail_snapshot_complete
                                CHECK (
                                    (
                                        calculation_months IS NULL
                                            AND base_asset IS NULL
                                            AND expected_salary IS NULL
                                            AND expected_spending IS NULL
                                            AND soldier_saving_principal IS NULL
                                            AND soldier_saving_interest IS NULL
                                            AND government_matching_support IS NULL
                                            AND investment_principal IS NULL
                                            AND expected_investment_return IS NULL
                                            AND unallocated_principal IS NULL
                                            AND calculation_policy_version IS NULL
                                        )
                                        OR
                                    (
                                        calculation_months IS NOT NULL
                                            AND base_asset IS NOT NULL
                                            AND expected_salary IS NOT NULL
                                            AND expected_spending IS NOT NULL
                                            AND soldier_saving_principal IS NOT NULL
                                            AND soldier_saving_interest IS NOT NULL
                                            AND government_matching_support IS NOT NULL
                                            AND investment_principal IS NOT NULL
                                            AND expected_investment_return IS NOT NULL
                                            AND unallocated_principal IS NOT NULL
                                            AND calculation_policy_version IS NOT NULL
                                        )
                                    )
) COMMENT='사용자 What-if 시뮬레이션 — 누적 저장(강사 피드백 반영), GET /simulations(목록)·GET /simulations/{id}(상세)로 재조회'
    DEFAULT CHARSET=utf8mb4
    COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------
-- 17. ai_analysis : AI 분석 이력
-- ---------------------------------------------
CREATE TABLE ai_analysis (
                             analysis_id     BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'AI 분석 ID',
                             user_id         BIGINT NOT NULL COMMENT '사용자 ID',
                             snapshot_id     BIGINT NULL COMMENT '분석 기준 자산 스냅샷 ID',
                             simulation_id   BIGINT NULL COMMENT '참조한 시뮬레이션 FK',
                             analysis_type   ENUM(
                                 'CONSUMPTION',
                                 'SAVING',
                                 'INVESTMENT',
                                 'POLICY',
                                 'DIAGNOSIS',
                                 'SCENARIO_COMPARISON'
                                 ) NOT NULL COMMENT '분석 유형',
                             result_json     JSON NOT NULL COMMENT 'AI 분석 결과',
                             input_data_hash CHAR(64) NULL COMMENT '분석 입력값 SHA-256 해시',
                             model_name      VARCHAR(100) NULL COMMENT 'AI 모델명',
                             prompt_version  VARCHAR(50) NULL COMMENT '프롬프트 버전',
                             generation_source ENUM('OPENAI', 'FALLBACK') NOT NULL COMMENT '원본 문구 생성 경로 — OpenAI 성공 또는 템플릿 대체; CACHE는 응답 시점 상태라 저장하지 않음',
                             created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',

                             CONSTRAINT fk_ai_analysis_user
                                 FOREIGN KEY (user_id) REFERENCES users(user_id)
                                     ON DELETE CASCADE,
                             CONSTRAINT fk_ai_analysis_snapshot
                                 FOREIGN KEY (snapshot_id) REFERENCES asset_snapshot(snapshot_id)
                                     ON DELETE SET NULL,
                             CONSTRAINT fk_ai_analysis_simulation
                                 FOREIGN KEY (simulation_id) REFERENCES simulation(simulation_id)
                                     ON DELETE SET NULL,
                             INDEX idx_ai_analysis_input_hash (input_data_hash)
) COMMENT='AI 분석 이력'
    DEFAULT CHARSET=utf8mb4
    COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------
-- 18. financial_product : 금융 상품
-- ---------------------------------------------
CREATE TABLE financial_product (
                                   product_id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '금융 상품 ID',
                                   product_name        VARCHAR(255) NOT NULL COMMENT '상품명',
                                   company_name        VARCHAR(100) NULL COMMENT '금융회사명',
                                   product_type        VARCHAR(50) NULL COMMENT '상품 유형',
                                   base_interest_rate  DECIMAL(5,2) NULL COMMENT '기본 금리',
                                   max_interest_rate   DECIMAL(5,2) NULL COMMENT '최고 금리',
                                   return_rate_1y      DECIMAL(6,2) NULL COMMENT '최근 1년 수익률',
                                   risk_category       ENUM('SAFE', 'AGGRESSIVE') NULL COMMENT '위험 분류',
                                   eligibility         TEXT NULL COMMENT '가입 조건',
                                   description         TEXT NULL COMMENT '상품 설명',
                                   source_url          VARCHAR(1000) NULL COMMENT '출처 URL',
                                   as_of_date          DATE NULL COMMENT '정보 기준일',
                                   status              ENUM('ACTIVE', 'EXPIRED') NOT NULL DEFAULT 'ACTIVE' COMMENT '상품 상태',
                                   created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',
                                   updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                                       ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 일시',

                                   CONSTRAINT uq_financial_product UNIQUE (product_name, company_name)
) COMMENT='금융 상품 — 적금·예금·ETF 등 투자상품 통합 관리(2026-07-24, 상품추천을 다시 MVP로 재격상하며 확장). CODEF 카탈로그에 대응 API 없음, 금융감독원·ETF CHECK류 외부 소스 필요'
    DEFAULT CHARSET=utf8mb4
    COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------
-- 19. military_benefit : 군인 혜택 및 청년 정책
-- ---------------------------------------------
CREATE TABLE military_benefit (
                                  benefit_id      BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '혜택 ID',
                                  title           VARCHAR(255) NOT NULL COMMENT '혜택명',
                                  discount_summary VARCHAR(500) NOT NULL DEFAULT '' COMMENT '카드 목록용 할인 요약',
                                  category        VARCHAR(50) NULL COMMENT '혜택 유형',
                                  description     TEXT NULL COMMENT '혜택 설명',
                                  target_rank     VARCHAR(30) NULL COMMENT '대상 계급',
                                  target_service  VARCHAR(30) NULL COMMENT '대상 군종',
                                  target_text     VARCHAR(500) NULL COMMENT '대상 안내',
                                  benefit_context ENUM('GENERAL', 'LEAVE') NOT NULL DEFAULT 'GENERAL' COMMENT '혜택 구분',
                                  card_company    VARCHAR(100) NULL COMMENT '카드사명',
                                  source_url      VARCHAR(1000) NULL COMMENT '출처 URL',
                                  effective_from  DATE NULL COMMENT '시행일',
                                  effective_to    DATE NULL COMMENT '종료일',
                                  usage_method    TEXT NULL COMMENT '이용 방법',
                                  precautions     TEXT NULL COMMENT '유의사항(전월 실적·월 한도·횟수 제한·급여이체 조건 등)',
                                  detail_image_url VARCHAR(1000) NULL COMMENT '상세 안내 이미지 URL',
                                  active_yn       BOOLEAN NOT NULL DEFAULT TRUE COMMENT '노출 여부',
                                  display_order   INT NOT NULL DEFAULT 0 COMMENT '카테고리 내 노출 순서',
                                  created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',
                                  updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                                      ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 일시',

                                  CONSTRAINT uq_military_benefit_context_category_title_order
                                      UNIQUE (benefit_context, category, title, display_order),
                                  INDEX idx_military_benefit_leave_lookup
                                      (benefit_context, active_yn, category, effective_from, effective_to)
) COMMENT='군인 혜택 및 청년 정책 — 휴가 혜택은 benefit_context=LEAVE로 관리'
    DEFAULT CHARSET=utf8mb4
    COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------
-- 20. product_recommendation : 금융 상품 추천
-- ---------------------------------------------
CREATE TABLE product_recommendation (
                                        recommendation_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '상품 추천 ID',
                                        user_id           BIGINT NOT NULL COMMENT '사용자 ID',
                                        product_id        BIGINT NOT NULL COMMENT '금융 상품 ID',
                                        recommend_reason  TEXT NULL COMMENT '추천 사유',
                                        created_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '추천 일시',

                                        CONSTRAINT uq_product_recommendation_user_product
                                            UNIQUE (user_id, product_id),
                                        CONSTRAINT fk_product_recommendation_user
                                            FOREIGN KEY (user_id) REFERENCES users(user_id)
                                                ON DELETE CASCADE,
                                        CONSTRAINT fk_product_recommendation_product
                                            FOREIGN KEY (product_id) REFERENCES financial_product(product_id)
                                                ON DELETE RESTRICT
) COMMENT='금융 상품 추천 — 규칙 기반 매칭(자격조건 필터+금리 정렬), AI 아님(2026-07-24 확정). recommend_reason은 템플릿 문자열. 2026-07-24 재확장: simulation.expected_return_rate 입력이 있으면 financial_product.return_rate_1y >= expected_return_rate 조건도 필터에 추가(ETF 등 투자상품 추천, MVP 재격상) — 이것도 단순 WHERE 필터라 규칙 기반 원칙 그대로 유지'
    DEFAULT CHARSET=utf8mb4
    COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------
-- 21. ai_recommended_scenario : AI 추천 시나리오
-- ---------------------------------------------
CREATE TABLE ai_recommended_scenario (
                                         scenario_id               BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'AI 추천 시나리오 ID',
                                         user_id                   BIGINT NOT NULL COMMENT '사용자 ID',
                                         monthly_saving_amount     BIGINT NOT NULL COMMENT '추천 장병내일준비적금 월 납입액(원, 0~550000)',
                                         monthly_investment_amount BIGINT NOT NULL COMMENT '추천 월 투자 배분액(원)',
                                         expected_return_rate       DECIMAL(5,2) NOT NULL COMMENT '목표 투자수익률(%, 연 환산)',
                                         monthly_spending_amount   BIGINT NOT NULL COMMENT '추천 월 소비액',
                                         expected_asset            BIGINT NOT NULL COMMENT '추천 전역 예상 자산',
                                         financial_discharge_date  DATE NULL COMMENT '추천 재정적 전역일',
                                         recommend_reason          TEXT NULL COMMENT '추천 사유',
                                         created_at                TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',

                                         CONSTRAINT fk_ai_recommended_scenario_user
                                             FOREIGN KEY (user_id) REFERENCES users(user_id)
                                                 ON DELETE CASCADE,
                                         CONSTRAINT chk_ai_recommended_scenario_monthly_amounts
                                             CHECK (
                                                 monthly_spending_amount >= 0
                                                     AND monthly_saving_amount BETWEEN 0 AND 550000
                                                     AND monthly_investment_amount >= 0
                                                 )
) COMMENT='AI 추천 시나리오 — 숫자 필드는 Spring 계산(결정론적), GPT는 recommend_reason 서술에만 선택적으로 관여(2026-07-24 확정)'
    DEFAULT CHARSET=utf8mb4
    COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------
-- 22. recurring_investment_plan : 사용자 적립식 위험자산 투자 계획
-- ---------------------------------------------
CREATE TABLE recurring_investment_plan (
                                           plan_id                  BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '적립식 투자 계획 ID',
                                           user_id                  BIGINT NOT NULL COMMENT '사용자 ID',
                                           brokerage_account_id     BIGINT NOT NULL COMMENT '사용자가 선택한 증권 계좌 ID',
                                           frequency                ENUM('WEEKLY', 'MONTHLY') NOT NULL COMMENT '적립 주기',
                                           contribution_day         TINYINT UNSIGNED NOT NULL COMMENT '주간 1(월)~7(일), 월간 1~28',
                                           contribution_amount      BIGINT NOT NULL COMMENT '회차당 현재 적립 예정 금액',
                                           maximum_monthly_amount   BIGINT NOT NULL COMMENT '사용자가 설정한 월 최대 투자한도',
                                           investment_product_code  VARCHAR(100) NOT NULL COMMENT '사용자가 선택한 투자 대상 코드',
                                           investment_product_name  VARCHAR(255) NOT NULL COMMENT '사용자가 선택한 투자 대상명',
                                           status                   ENUM('ACTIVE', 'PAUSED', 'SAFE_FOCUS') NOT NULL DEFAULT 'ACTIVE' COMMENT '내부 적립 계획 상태',
                                           next_contribution_date   DATE NULL COMMENT '다음 내부 적립 예정일',
                                           created_at               TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',
                                           updated_at               TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                                               ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 일시',

                                           CONSTRAINT uq_recurring_investment_plan_user UNIQUE (user_id),
                                           CONSTRAINT fk_recurring_investment_plan_user
                                               FOREIGN KEY (user_id) REFERENCES users(user_id)
                                                   ON DELETE CASCADE,
                                           CONSTRAINT fk_recurring_investment_plan_account
                                               FOREIGN KEY (brokerage_account_id) REFERENCES connected_account(account_id)
                                                   ON DELETE RESTRICT,
                                           CONSTRAINT chk_recurring_investment_plan_amounts
                                               CHECK (contribution_amount >= 0 AND maximum_monthly_amount >= 0),
                                           CONSTRAINT chk_recurring_investment_plan_day
                                               CHECK (
                                                   (frequency = 'WEEKLY' AND contribution_day BETWEEN 1 AND 7)
                                                       OR (frequency = 'MONTHLY' AND contribution_day BETWEEN 1 AND 28)
                                                   )
) COMMENT='사용자가 직접 설정한 정기 위험자산 적립 계획. 실제 증권 주문은 수행하지 않음'
    DEFAULT CHARSET=utf8mb4
    COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------
-- 23. investment_guidance : 목표 기반 다음 적립금 가이드
-- ---------------------------------------------
CREATE TABLE investment_guidance (
                                     guidance_id                     BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '투자 가이드 ID',
                                     user_id                         BIGINT NOT NULL COMMENT '사용자 ID',
                                     plan_id                         BIGINT NOT NULL COMMENT '계산에 사용한 적립 계획 ID',
                                     forecast_id                     BIGINT NOT NULL COMMENT '계산에 사용한 캐시플로우 예측 ID',
                                     plan_updated_at                 TIMESTAMP NOT NULL COMMENT '계산에 사용한 적립 계획 버전 시각',
                                     input_data_hash                 CHAR(64) NOT NULL COMMENT '계획·목표·예측·증권평가 입력 해시',
                                     service_stage                   ENUM('PRIVATE_BASE', 'PRIVATE_FIRST_CLASS_GROWTH', 'CORPORAL_CHECK', 'SERGEANT_PREPARE') NOT NULL COMMENT '계급 기반 UI 여정 단계',
                                     action_type                     ENUM('START', 'CONTINUE', 'REDUCE', 'PAUSE', 'SAFE_FOCUS', 'REVIEW') NOT NULL COMMENT '다음 적립 회차 행동 가이드',
                                     target_amount                   BIGINT NOT NULL COMMENT '전역 목표금액',
                                     current_contribution_amount     BIGINT NOT NULL COMMENT '현재 회차당 적립금',
                                     recommended_contribution_amount BIGINT NOT NULL COMMENT '추천 회차당 적립금',
                                     continue_expected_asset         BIGINT NOT NULL COMMENT '현재 계획 유지 시 전역 예상자산',
                                     recommended_expected_asset      BIGINT NOT NULL COMMENT '추천 계획 적용 시 전역 예상자산',
                                     investment_principal            BIGINT NOT NULL DEFAULT 0 COMMENT '선택 투자대상의 매입원금',
                                     market_value                    BIGINT NOT NULL DEFAULT 0 COMMENT '선택 투자대상의 평가금액',
                                     unrealized_profit_loss          BIGINT NOT NULL DEFAULT 0 COMMENT '선택 투자대상의 평가손익',
                                     return_rate                     DECIMAL(9,4) NOT NULL DEFAULT 0 COMMENT '선택 투자대상의 수익률(%)',
                                     safe_asset_amount               BIGINT NULL COMMENT '가이드 계산 당시 증권계좌 예수금(안전자산)',
                                     risk_asset_amount               BIGINT NULL COMMENT '가이드 계산 당시 증권계좌 전체 보유종목 평가액(위험자산)',
                                     expected_return_rate            DECIMAL(5,2) NOT NULL DEFAULT 0 COMMENT '결정론 계산에 사용한 연 예상수익률(%)',
                                     remaining_contribution_count    INT NOT NULL DEFAULT 0 COMMENT '전역일까지 남은 적립 회차 수',
                                     safety_buffer_amount            BIGINT NOT NULL DEFAULT 0 COMMENT 'SAFE_FOCUS 판정용 안전 여유금',
                                     reason                          TEXT NOT NULL COMMENT '가이드 설명. 숫자와 행동은 Spring이 결정',
                                     market_data_as_of               TIMESTAMP NULL COMMENT '증권 평가 데이터 기준시각',
                                     next_review_at                  TIMESTAMP NOT NULL COMMENT '다음 가이드 계산 예정시각',
                                     created_at                      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',

                                     INDEX idx_investment_guidance_user_created (user_id, created_at, guidance_id),
                                     INDEX idx_investment_guidance_user_hash (user_id, input_data_hash),
                                     CONSTRAINT fk_investment_guidance_user
                                         FOREIGN KEY (user_id) REFERENCES users(user_id)
                                             ON DELETE CASCADE,
                                     CONSTRAINT fk_investment_guidance_plan
                                         FOREIGN KEY (plan_id) REFERENCES recurring_investment_plan(plan_id)
                                             ON DELETE CASCADE,
                                     CONSTRAINT fk_investment_guidance_forecast
                                         FOREIGN KEY (forecast_id) REFERENCES cashflow_forecast(forecast_id)
                                             ON DELETE RESTRICT,
                                     CONSTRAINT chk_investment_guidance_amounts
                                         CHECK (
                                             target_amount >= 0
                                                 AND current_contribution_amount >= 0
                                                 AND recommended_contribution_amount >= 0
                                                 AND remaining_contribution_count >= 0
                                                 AND safety_buffer_amount >= 0
                                             )
) COMMENT='증권 평가와 전역 목표를 반영한 다음 적립금 가이드. 자동 매도·자동 주문 없음'
    DEFAULT CHARSET=utf8mb4
    COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------
-- 24. strategy_application : 전략 적용 이력
-- ---------------------------------------------
CREATE TABLE strategy_application (
                                      application_id                   BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '전략 적용 ID',
                                      user_id                          BIGINT NOT NULL COMMENT '사용자 ID',
                                      source_type                      ENUM('SIMULATION', 'AI_RECOMMENDATION', 'INVESTMENT_GUIDANCE', 'MANUAL') NOT NULL COMMENT '적용 출처',
                                      analysis_id                      BIGINT NULL COMMENT '원본 AI 분석 ID — AI 추천 적용 멱등성 키',
                                      simulation_id                    BIGINT NULL COMMENT '원본 시뮬레이션 ID',
                                      ai_scenario_id                   BIGINT NULL COMMENT '원본 AI 추천 시나리오 ID',
                                      guidance_id                      BIGINT NULL COMMENT '원본 적립식 투자 가이드 ID',
                                      applied_guidance_action          ENUM('START', 'CONTINUE', 'REDUCE', 'PAUSE', 'SAFE_FOCUS') NULL COMMENT '사용자가 실제 선택한 가이드 행동',
                                      applied_investment_frequency     ENUM('WEEKLY', 'MONTHLY') NULL COMMENT '적용한 적립 주기',
                                      applied_recurring_contribution_amount BIGINT NULL COMMENT '적용한 회차당 위험자산 적립금',
                                      applied_monthly_saving_amount    BIGINT NULL COMMENT '적용 장병내일준비적금 월 납입액(원, 0~550000)',
                                      applied_monthly_investment_amount BIGINT NULL COMMENT '적용 월 투자 배분액(원)',
                                      applied_expected_return_rate     DECIMAL(5,2) NULL COMMENT '적용 목표 투자수익률',
                                      applied_monthly_spending_amount  BIGINT NULL COMMENT '적용 월 소비액',
                                      before_expected_asset            BIGINT NULL COMMENT '적용 전 예상 자산',
                                      after_expected_asset             BIGINT NULL COMMENT '적용 후 예상 자산',
                                      applied_at                       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '적용 일시',

                                      CONSTRAINT fk_strategy_application_user
                                          FOREIGN KEY (user_id) REFERENCES users(user_id)
                                              ON DELETE CASCADE,
                                      CONSTRAINT fk_strategy_application_analysis
                                          FOREIGN KEY (analysis_id) REFERENCES ai_analysis(analysis_id)
                                              ON DELETE SET NULL,
                                      CONSTRAINT fk_strategy_application_simulation
                                          FOREIGN KEY (simulation_id) REFERENCES simulation(simulation_id)
                                              ON DELETE SET NULL,
                                      CONSTRAINT fk_strategy_application_ai_scenario
                                          FOREIGN KEY (ai_scenario_id) REFERENCES ai_recommended_scenario(scenario_id)
                                              ON DELETE SET NULL,
                                      CONSTRAINT fk_strategy_application_guidance
                                          FOREIGN KEY (guidance_id) REFERENCES investment_guidance(guidance_id)
                                              ON DELETE SET NULL,
                                      CONSTRAINT uq_strategy_application_guidance_selection
                                          UNIQUE (user_id, guidance_id, applied_guidance_action, applied_investment_frequency, applied_recurring_contribution_amount),
                                      CONSTRAINT uq_strategy_application_ai_analysis
                                          UNIQUE (user_id, analysis_id),
                                      CONSTRAINT chk_strategy_application_monthly_amounts
                                          CHECK (
                                              (applied_monthly_spending_amount IS NULL OR applied_monthly_spending_amount >= 0)
                                                  AND (applied_monthly_saving_amount IS NULL OR applied_monthly_saving_amount BETWEEN 0 AND 550000)
                                                  AND (applied_monthly_investment_amount IS NULL OR applied_monthly_investment_amount >= 0)
                                              ),
                                      CONSTRAINT chk_strategy_application_recurring_amount
                                          CHECK (
                                              applied_recurring_contribution_amount IS NULL
                                                  OR applied_recurring_contribution_amount >= 0
                                              )
) COMMENT='최신 AI 추천 적용 행은 활성 캐시플로우 전략이며 전체 행은 적용 감사 이력. 실제 금융 주문은 수행하지 않음'
    DEFAULT CHARSET=utf8mb4
    COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------
-- 25. discharge_report : 전역 리포트
-- ---------------------------------------------
CREATE TABLE discharge_report (
                                  report_id             BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '전역 리포트 ID',
                                  user_id               BIGINT NOT NULL COMMENT '사용자 ID',
                                  start_asset           BIGINT NOT NULL DEFAULT 0 COMMENT '입대 시 자산',
                                  final_asset           BIGINT NOT NULL DEFAULT 0 COMMENT '전역 시 자산',
                                  growth_rate           DECIMAL(5,2) NULL COMMENT '자산 증가율',
                                  goal_achievement_rate DECIMAL(5,2) NULL COMMENT '목표 달성률',
                                  report_json           JSON NULL COMMENT '전역 리포트 상세 결과',
                                  created_at            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',

                                  CONSTRAINT uq_discharge_report_user UNIQUE (user_id),
                                  CONSTRAINT fk_discharge_report_user
                                      FOREIGN KEY (user_id) REFERENCES users(user_id)
                                          ON DELETE CASCADE
) COMMENT='전역 리포트 — 전역 시점 1회성 결과물, 유저당 1행(2026-07-22 확정)'
    DEFAULT CHARSET=utf8mb4
    COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------
-- 26. refresh_token : JWT Refresh Token 관리
-- ---------------------------------------------
CREATE TABLE refresh_token (
                               token_id    BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '토큰 ID',
                               user_id     BIGINT NOT NULL COMMENT '사용자 ID',
                               token_hash  CHAR(64) NOT NULL COMMENT 'Refresh Token SHA-256 해시값',
                               user_agent  VARCHAR(255) NULL COMMENT '발급 기기 정보(OS, 브라우저 등)',
                               expires_at  DATETIME NOT NULL COMMENT '만료 일시',
                               created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',

                               CONSTRAINT uq_refresh_token_hash UNIQUE (token_hash),
                               CONSTRAINT fk_refresh_token_user
                                   FOREIGN KEY (user_id) REFERENCES users(user_id)
                                       ON DELETE CASCADE
) COMMENT='JWT Refresh Token 관리'
    DEFAULT CHARSET=utf8mb4
    COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------
-- 27. mission : 미션 마스터
-- ---------------------------------------------
CREATE TABLE mission (
                         mission_id      BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '미션 ID',
                         mission_code    VARCHAR(50) NULL COMMENT '변경되지 않는 미션 식별 코드',
                         mission_type    ENUM('SAFE', 'AGGRESSIVE') NULL COMMENT '미션 성향(공통 미션은 NULL)',
                         mission_category ENUM('DAILY', 'RECOMMENDED', 'ONE_TIME', 'EVENT') NOT NULL DEFAULT 'DAILY' COMMENT '미션 노출 분류',
                         title           VARCHAR(255) NOT NULL COMMENT '미션명 (예: 30일 연속 출석 체크, ETF 첫 투자 미션)',
                         description     TEXT NULL COMMENT '미션 설명',
                         action_type     VARCHAR(50) NOT NULL COMMENT '완료 조건 유형 (ATTENDANCE, PRODUCT_VIEW, SAVING_CHECK, SIMULATION_RUN 등)',
                         display_order   INT NOT NULL DEFAULT 0 COMMENT '화면 노출 순서',
                         trigger_type    ENUM('NONE', 'DAYS_TO_DISCHARGE', 'LEAVE_SCHEDULED', 'PAYDAY') NOT NULL DEFAULT 'NONE' COMMENT '이벤트 미션 노출 조건 유형',
                         trigger_value   INT NULL COMMENT '이벤트 조건값(예: 전역까지 남은 일수)',
                         event_priority  INT NOT NULL DEFAULT 0 COMMENT '동시 이벤트 발생 시 노출 우선순위',
                         is_active       BOOLEAN NOT NULL DEFAULT TRUE COMMENT '활성 여부',
                         created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',

                         CONSTRAINT uq_mission_code
                             UNIQUE (mission_code),
                         CONSTRAINT chk_mission_display_order
                             CHECK (display_order >= 0),
                         CONSTRAINT chk_mission_trigger_value
                             CHECK (trigger_value IS NULL OR trigger_value >= 0),
                         CONSTRAINT chk_mission_event_priority
                             CHECK (event_priority >= 0),
                         CONSTRAINT chk_mission_recommended_type
                             CHECK (
                                 mission_category <> 'RECOMMENDED'
                                     OR mission_type IS NOT NULL
                                 ),
                         CONSTRAINT chk_mission_daily_common_type
                             CHECK (
                                 mission_category <> 'DAILY'
                                     OR mission_type IS NULL
                                 ),
                         CONSTRAINT chk_mission_event_trigger
                             CHECK (
                                 mission_category <> 'EVENT'
                                     OR trigger_type <> 'NONE'
                                 )
) COMMENT='공통 데일리·성향별 추천·1회성·조건형 이벤트 미션을 관리하는 마스터'
    DEFAULT CHARSET=utf8mb4
    COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------
-- 28. user_mission_completion : 사용자별 미션 완료 이력
-- ---------------------------------------------
CREATE TABLE user_mission_completion (
                                         completion_id     BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '미션 완료 ID',
                                         user_id            BIGINT NOT NULL COMMENT '사용자 ID',
                                         mission_id         BIGINT NOT NULL COMMENT '미션 ID',
                                         completion_date    DATE NOT NULL COMMENT '완료 적용일',
                                         completed_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '실제 완료 일시',

                                         CONSTRAINT uq_user_mission_completion
                                             UNIQUE (user_id, mission_id, completion_date),
                                         CONSTRAINT fk_user_mission_completion_user
                                             FOREIGN KEY (user_id) REFERENCES users(user_id)
                                                 ON DELETE CASCADE,
                                         CONSTRAINT fk_user_mission_completion_mission
                                             FOREIGN KEY (mission_id) REFERENCES mission(mission_id)
                                                 ON DELETE CASCADE
) COMMENT='미션 완료 이력'
    DEFAULT CHARSET=utf8mb4
    COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------
-- 29. badge : 뱃지 마스터
-- ---------------------------------------------
CREATE TABLE badge (
                       badge_id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '뱃지 ID',
                       badge_name        VARCHAR(100) NOT NULL COMMENT '뱃지명',
                       badge_description VARCHAR(500) NULL COMMENT '뱃지 획득 조건 설명',
                       mission_type      ENUM('SAFE', 'AGGRESSIVE') NOT NULL COMMENT '뱃지 투자 성향',
                       required_completion_count INT NOT NULL COMMENT '티어 획득에 필요한 누적 미션 완료 수',
                       grade             ENUM('BRONZE', 'SILVER', 'GOLD', 'PLATINUM', 'DIAMOND') NOT NULL COMMENT '뱃지 등급',
                       is_active         BOOLEAN NOT NULL DEFAULT TRUE COMMENT '활성 여부',
                       created_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',

                       CONSTRAINT uq_badge_name UNIQUE (badge_name),
                       CONSTRAINT uq_badge_mission_type_grade
                           UNIQUE (mission_type, grade),
                       CONSTRAINT uq_badge_mission_type_completion_count
                           UNIQUE (mission_type, required_completion_count),
                       CONSTRAINT chk_badge_required_completion_count
                           CHECK (required_completion_count > 0)
) COMMENT='성향별 누적 미션 완료 수 티어를 정의하는 뱃지 마스터'
    DEFAULT CHARSET=utf8mb4
    COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------
-- 30. investment_badge : 투자 뱃지
-- ---------------------------------------------
CREATE TABLE user_badge (
                            user_badge_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '사용자 뱃지 이력 ID',
                            user_id       BIGINT NOT NULL COMMENT '사용자 ID',
                            badge_id      BIGINT NOT NULL COMMENT '뱃지 ID',
                            acquired_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '획득 일시',

                            CONSTRAINT uq_user_badge_user_badge UNIQUE (user_id, badge_id),
                            CONSTRAINT fk_user_badge_user
                                FOREIGN KEY (user_id) REFERENCES users(user_id)
                                    ON DELETE CASCADE,
                            CONSTRAINT fk_user_badge_badge
                                FOREIGN KEY (badge_id) REFERENCES badge(badge_id)
                                    ON DELETE CASCADE
) COMMENT='사용자별 투자 뱃지 획득 이력'
    DEFAULT CHARSET=utf8mb4
    COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------
-- 사용자별 투자 뱃지 집계
-- ---------------------------------------------
CREATE TABLE investment_badge (
                                  badge_id            BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '투자 뱃지 ID',
                                  user_id             BIGINT NOT NULL COMMENT '사용자 ID',
                                  initial_preference  ENUM('SAFE', 'BALANCED', 'AGGRESSIVE') NULL COMMENT '온보딩 선택 시드값',
                                  badge_tier          ENUM('SAFE', 'BALANCED', 'AGGRESSIVE') NULL COMMENT '뱃지 유형',
                                  safe_count          INT NOT NULL DEFAULT 0 COMMENT '안정형 미션 누적 완료 수',
                                  safe_grade          ENUM('BRONZE', 'SILVER', 'GOLD', 'PLATINUM', 'DIAMOND') NULL COMMENT '안정형 누적 등급',
                                  aggressive_count    INT NOT NULL DEFAULT 0 COMMENT '공격형 미션 누적 완료 수',
                                  aggressive_grade    ENUM('BRONZE', 'SILVER', 'GOLD', 'PLATINUM', 'DIAMOND') NULL COMMENT '공격형 누적 등급',
                                  updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 일시',

                                  CONSTRAINT uq_investment_badge_user
                                      UNIQUE (user_id),
                                  CONSTRAINT fk_investment_badge_user
                                      FOREIGN KEY (user_id) REFERENCES users(user_id)
                                          ON DELETE CASCADE
) COMMENT='투자 뱃지 — 미션 완료 기반, 실시간 갱신(2026-07-25, 저녁 균형형 판정+콜드스타트 시드 추가)'
    DEFAULT CHARSET=utf8mb4
    COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------
-- 30. leave_mode : 휴가모드
-- ---------------------------------------------
CREATE TABLE leave_mode (
                            leave_mode_id   BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '휴가모드 ID',
                            user_id         BIGINT NOT NULL COMMENT '사용자 ID',
                            event_name      VARCHAR(100) NOT NULL COMMENT '이벤트명(휴가, 외출, 외박 등)',
                            start_date      DATE NOT NULL COMMENT '휴가 시작일',
                            end_date        DATE NOT NULL COMMENT '휴가 종료일',
                            is_leave_mode_enabled BOOLEAN NOT NULL DEFAULT TRUE COMMENT '일정 기간 휴가모드 자동 전환 여부',
                            deleted_at      TIMESTAMP NULL COMMENT '삭제 일시',
                            budget_amount   BIGINT NULL COMMENT '휴가 예산 설정값(선택 입력)',
                            created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',
                            updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 일시',

                            INDEX idx_leave_mode_user_start_date (user_id, start_date),
                            INDEX idx_leave_mode_user_enabled_period
                                (user_id, is_leave_mode_enabled, start_date, end_date),
                            INDEX idx_leave_mode_user_deleted_start_date
                                (user_id, deleted_at, start_date),
                            CONSTRAINT chk_leave_mode_period
                                CHECK (start_date <= end_date),
                            CONSTRAINT chk_leave_mode_budget
                                CHECK (budget_amount IS NULL OR budget_amount >= 0),
                            CONSTRAINT fk_leave_mode_user
                                FOREIGN KEY (user_id) REFERENCES users(user_id)
                                    ON DELETE CASCADE
) COMMENT='사용자 일정 및 휴가모드 자동 전환'
    DEFAULT CHARSET=utf8mb4
    COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------
-- 31. daily_market_report : 오늘의 AI 시장 리포트
-- ---------------------------------------------
CREATE TABLE daily_market_report (
                                     report_id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '오늘의 리포트 ID',
                                     report_date        DATE NOT NULL COMMENT '서비스 기준일(17:00~익일 16:59:59 노출 구간의 기준 날짜)',
                                     title              VARCHAR(200) NOT NULL COMMENT '오늘의 AI 시장 리포트 제목',
                                     summary            VARCHAR(500) NOT NULL COMMENT '오늘의 AI 시장 리포트 한줄 요약',
                                     content            TEXT NOT NULL COMMENT '선별된 Finnhub 뉴스에 근거해 Gemini가 생성한 사실 기반 시장 리포트 본문',
                                     report_status      ENUM('NORMAL', 'PARTIAL', 'STALE') NOT NULL DEFAULT 'NORMAL' COMMENT '리포트 전체 상태 — PARTIAL: 일부 지표 DELAYED/MISSING, STALE: 당일 배치 실패로 이전 리포트 노출 중',
                                     generation_source  ENUM('GEMINI', 'FALLBACK') NOT NULL COMMENT '본문 생성 경로 — Gemini 성공 또는 안전한 대체 상태',
                                     model_name         VARCHAR(100) NOT NULL COMMENT '생성에 사용한 모델명(gemini-3.6-flash)',
                                     prompt_version     VARCHAR(100) NOT NULL COMMENT 'Gemini Interactions 프롬프트 버전',
                                     valid_from         TIMESTAMP NOT NULL COMMENT '노출 시작 시각(해당일 17:00)',
                                     valid_until        TIMESTAMP NOT NULL COMMENT '노출 종료 시각(익일 16:59:59)',
                                     created_at         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',

                                     CONSTRAINT uq_daily_market_report_date
                                         UNIQUE (report_date)
) COMMENT='오늘의 AI 시장 리포트 — 전체 사용자 공통 1일 1건'
    DEFAULT CHARSET=utf8mb4
    COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------
-- 31-B. daily_market_indicator : 오늘의 AI 시장 리포트 지표 원본값
-- ---------------------------------------------
CREATE TABLE daily_market_indicator (
                                        indicator_id     BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '지표 ID',
                                        report_id        BIGINT NOT NULL COMMENT '소속 리포트',
                                        indicator_type   ENUM('KOSPI', 'KOSDAQ', 'US_TREASURY_10Y', 'USD_KRW') NOT NULL COMMENT '지표 종류',
                                        data_as_of       TIMESTAMP NULL COMMENT '해당 지표 값의 실제 기준 시각(MISSING이면 NULL)',
                                        source           VARCHAR(100) NOT NULL COMMENT '제공처명',
                                        observed_value   DECIMAL(18,4) NULL COMMENT '관측값(지수·금리·환율, MISSING이면 NULL)',
                                        change_value     DECIMAL(18,4) NULL COMMENT '전일 대비 변화량',
                                        change_rate      DECIMAL(6,2) NULL COMMENT '전일 대비 변화율(%)',
                                        status           ENUM('NORMAL', 'DELAYED', 'MISSING') NOT NULL COMMENT '지표 단위 수집 상태',
                                        created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',

                                        CONSTRAINT uq_daily_market_indicator
                                            UNIQUE (report_id, indicator_type),
                                        CONSTRAINT fk_daily_market_indicator_report
                                            FOREIGN KEY (report_id) REFERENCES daily_market_report (report_id)
                                                ON DELETE CASCADE
) COMMENT='오늘의 AI 시장 리포트 지표별 원본값 — 리포트 1건당 4행'
    DEFAULT CHARSET=utf8mb4
    COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------
-- 31-C. daily_market_report_source : 오늘의 AI 시장 리포트 인용 출처
-- ---------------------------------------------
CREATE TABLE daily_market_report_source (
                                            source_id       BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '출처 ID',
                                            report_id       BIGINT NOT NULL COMMENT '소속 리포트',
                                            source_order    SMALLINT UNSIGNED NOT NULL COMMENT '리포트 응답에 노출할 출처 순서',
                                            title           VARCHAR(500) NOT NULL COMMENT '인용 출처 제목',
                                            url             VARCHAR(2048) NOT NULL COMMENT '인용 출처 URL(http/https만 허용)',
                                            created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',

                                            CONSTRAINT uq_daily_market_report_source_order
                                                UNIQUE (report_id, source_order),
                                            CONSTRAINT chk_daily_market_report_source_url
                                                CHECK (LOWER(url) REGEXP '^(http|https)://'),
                                            CONSTRAINT fk_daily_market_report_source_report
                                                FOREIGN KEY (report_id) REFERENCES daily_market_report (report_id)
                                                    ON DELETE CASCADE
) COMMENT='오늘의 AI 시장 리포트가 사용한 인용 출처 메타데이터 — 기사 전문은 저장하지 않음'
    DEFAULT CHARSET=utf8mb4
    COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------
-- 32. device_token : FCM 디바이스 토큰
-- ---------------------------------------------
CREATE TABLE device_token (
                              device_token_id  BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '디바이스 토큰 ID',
                              user_id          BIGINT NOT NULL COMMENT '사용자 ID',
                              fcm_token        VARCHAR(500) NOT NULL COMMENT 'FCM 디바이스 토큰',
                              device_type      ENUM('IOS', 'ANDROID', 'WEB') NOT NULL COMMENT '디바이스 유형',
                              is_active        BOOLEAN NOT NULL DEFAULT TRUE COMMENT '활성 여부',
                              created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',
                              updated_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 일시',

                              INDEX idx_device_token_user_active (user_id, is_active),

                              CONSTRAINT uq_device_token_fcm
                                  UNIQUE (fcm_token),
                              CONSTRAINT fk_device_token_user
                                  FOREIGN KEY (user_id) REFERENCES users(user_id)
                                      ON DELETE CASCADE
) COMMENT='FCM 디바이스 토큰 관리'
    DEFAULT CHARSET=utf8mb4
    COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------
-- 33. notification_history : 사용자별 알림 및 Push 발송 상태
-- ---------------------------------------------
CREATE TABLE notification_history (
                                      notification_id    BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '알림 ID',
                                      user_id             BIGINT NOT NULL COMMENT '사용자 ID',
                                      notification_type   VARCHAR(50) NOT NULL COMMENT '알림 유형',
                                      title                VARCHAR(255) NOT NULL COMMENT '알림 제목',
                                      body                 VARCHAR(500) NULL COMMENT '알림 본문',
                                      deep_link            VARCHAR(500) NULL COMMENT '클라이언트 이동 경로',
                                      dedupe_key           VARCHAR(200) NOT NULL COMMENT '알림 중복 방지 키',
                                      is_read              BOOLEAN NOT NULL DEFAULT FALSE COMMENT '읽음 여부',
                                      push_status          ENUM('PENDING', 'PROCESSING', 'SENT', 'FAILED', 'SKIPPED') NOT NULL DEFAULT 'PENDING' COMMENT 'Push 발송 상태',
                                      push_attempts        INT NOT NULL DEFAULT 0 COMMENT 'Push 발송 시도 횟수',
                                      sent_at              TIMESTAMP NULL COMMENT 'FCM 발송 완료 일시',
                                      read_at              TIMESTAMP NULL COMMENT '읽은 일시',
                                      created_at           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',
                                      updated_at           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 일시',

                                      CONSTRAINT uq_notification_history_dedupe UNIQUE (dedupe_key),
                                      INDEX idx_notification_history_user_created (user_id, created_at DESC, notification_id DESC),
                                      INDEX idx_notification_history_user_unread (user_id, is_read),

                                      CONSTRAINT fk_notification_history_user
                                          FOREIGN KEY (user_id) REFERENCES users(user_id)
                                              ON DELETE CASCADE
) COMMENT='개인 알림함과 FCM 발송 상태'
    DEFAULT CHARSET=utf8mb4
    COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------
-- 34. notification_campaign : 전체 사용자 공통 알림
-- ---------------------------------------------
CREATE TABLE notification_campaign (
                                       campaign_id         BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '전체 알림 ID',
                                       notification_type   VARCHAR(50) NOT NULL COMMENT '알림 유형',
                                       title               VARCHAR(255) NOT NULL COMMENT '알림 제목',
                                       body                VARCHAR(500) NULL COMMENT '알림 본문',
                                       deep_link           VARCHAR(500) NULL COMMENT '클라이언트 이동 경로',
                                       topic               VARCHAR(200) NOT NULL COMMENT 'FCM Topic',
                                       dedupe_key          VARCHAR(200) NOT NULL COMMENT '중복 방지 키',
                                       push_status         ENUM('PENDING', 'PROCESSING', 'SENT', 'FAILED', 'SKIPPED') NOT NULL DEFAULT 'PENDING' COMMENT 'Push 발송 상태',
                                       push_attempts       INT NOT NULL DEFAULT 0 COMMENT 'Push 발송 시도 횟수',
                                       visible_from        TIMESTAMP NOT NULL COMMENT '알림함 노출 시작',
                                       visible_until       TIMESTAMP NULL COMMENT '알림함 노출 종료',
                                       sent_at             TIMESTAMP NULL COMMENT 'FCM 발송 완료 일시',
                                       created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',
                                       updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 일시',

                                       CONSTRAINT uq_notification_campaign_dedupe UNIQUE (dedupe_key),
                                       INDEX idx_notification_campaign_visible (visible_from, visible_until, created_at)
) COMMENT='Topic으로 발송하고 사용자별 행을 미리 만들지 않는 공통 알림'
    DEFAULT CHARSET=utf8mb4
    COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------
-- 35. notification_campaign_receipt : 공통 알림 읽음 상태
-- ---------------------------------------------
CREATE TABLE notification_campaign_receipt (
                                               campaign_id BIGINT NOT NULL COMMENT '전체 알림 ID',
                                               user_id     BIGINT NOT NULL COMMENT '사용자 ID',
                                               read_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '읽은 일시',

                                               PRIMARY KEY (campaign_id, user_id),
                                               CONSTRAINT fk_notification_campaign_receipt_campaign
                                                   FOREIGN KEY (campaign_id) REFERENCES notification_campaign(campaign_id)
                                                       ON DELETE CASCADE,
                                               CONSTRAINT fk_notification_campaign_receipt_user
                                                   FOREIGN KEY (user_id) REFERENCES users(user_id)
                                                       ON DELETE CASCADE
) COMMENT='전체 공통 알림의 사용자별 읽음 상태'
    DEFAULT CHARSET=utf8mb4
    COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------
-- 36. notification_outbox : Redis Stream 발행 원본
-- ---------------------------------------------
CREATE TABLE notification_outbox (
                                     outbox_id        BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'Outbox ID',
                                     event_id         CHAR(36) NOT NULL COMMENT '이벤트 UUID',
                                     notification_id  BIGINT NULL COMMENT '개인 알림 ID',
                                     campaign_id      BIGINT NULL COMMENT '전체 알림 ID',
                                     dedupe_key       VARCHAR(200) NOT NULL COMMENT 'Outbox 중복 방지 키',
                                     status           ENUM('PENDING', 'PROCESSING', 'PUBLISHED', 'FAILED') NOT NULL DEFAULT 'PENDING' COMMENT 'Stream 발행 상태',
                                     publish_attempts INT NOT NULL DEFAULT 0 COMMENT 'Stream 발행 시도 횟수',
                                     available_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '다음 발행 가능 시각',
                                     claimed_at       TIMESTAMP NULL COMMENT 'Publisher claim 시각',
                                     published_at     TIMESTAMP NULL COMMENT 'Stream 발행 완료 시각',
                                     last_error       VARCHAR(500) NULL COMMENT '최근 발행 실패 원인',
                                     created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',
                                     updated_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 일시',

                                     CONSTRAINT uq_notification_outbox_event UNIQUE (event_id),
                                     CONSTRAINT uq_notification_outbox_dedupe UNIQUE (dedupe_key),
                                     INDEX idx_notification_outbox_publish (status, available_at, outbox_id),
                                     CONSTRAINT chk_notification_outbox_target
                                         CHECK ((notification_id IS NOT NULL) <> (campaign_id IS NOT NULL)),
                                     CONSTRAINT fk_notification_outbox_notification
                                         FOREIGN KEY (notification_id) REFERENCES notification_history(notification_id)
                                             ON DELETE CASCADE,
                                     CONSTRAINT fk_notification_outbox_campaign
                                         FOREIGN KEY (campaign_id) REFERENCES notification_campaign(campaign_id)
                                             ON DELETE CASCADE
) COMMENT='DB 트랜잭션과 Redis XADD 사이의 유실을 막는 Transactional Outbox'
    DEFAULT CHARSET=utf8mb4
    COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- Operational reference seed: sql/seed/military_pay_policy.sql
-- ============================================================
-- 캐시플로우용 병 봉급 정책 초기 데이터
-- 실행: mysql -u <DB_USER> -p <DB_NAME> < sql/seed/military_pay_policy.sql
--
-- 2023~2026년 계급별 봉급 기준값을 모든 군종에 공통 적용한다.
-- 연도별 정책은 해당 연도 1월 1일에 시작하고, 과거 연도는 12월 31일에 종료한다.

SET NAMES utf8mb4;
SET time_zone = '+09:00';

START TRANSACTION;

INSERT INTO military_pay_policy (
    soldier_type,
    rank_name,
    effective_year,
    monthly_salary,
    effective_from,
    effective_to
) VALUES
      ('ARMY', '이병', 2023, 600000, '2023-01-01', '2023-12-31'),
      ('ARMY', '일병', 2023, 680000, '2023-01-01', '2023-12-31'),
      ('ARMY', '상병', 2023, 800000, '2023-01-01', '2023-12-31'),
      ('ARMY', '병장', 2023, 1000000, '2023-01-01', '2023-12-31'),
      ('ARMY', '이병', 2024, 640000, '2024-01-01', '2024-12-31'),
      ('ARMY', '일병', 2024, 800000, '2024-01-01', '2024-12-31'),
      ('ARMY', '상병', 2024, 1000000, '2024-01-01', '2024-12-31'),
      ('ARMY', '병장', 2024, 1250000, '2024-01-01', '2024-12-31'),
      ('ARMY', '이병', 2025, 750000, '2025-01-01', '2025-12-31'),
      ('ARMY', '일병', 2025, 900000, '2025-01-01', '2025-12-31'),
      ('ARMY', '상병', 2025, 1200000, '2025-01-01', '2025-12-31'),
      ('ARMY', '병장', 2025, 1500000, '2025-01-01', '2025-12-31'),
      ('NAVY', '이병', 2023, 600000, '2023-01-01', '2023-12-31'),
      ('NAVY', '일병', 2023, 680000, '2023-01-01', '2023-12-31'),
      ('NAVY', '상병', 2023, 800000, '2023-01-01', '2023-12-31'),
      ('NAVY', '병장', 2023, 1000000, '2023-01-01', '2023-12-31'),
      ('NAVY', '이병', 2024, 640000, '2024-01-01', '2024-12-31'),
      ('NAVY', '일병', 2024, 800000, '2024-01-01', '2024-12-31'),
      ('NAVY', '상병', 2024, 1000000, '2024-01-01', '2024-12-31'),
      ('NAVY', '병장', 2024, 1250000, '2024-01-01', '2024-12-31'),
      ('NAVY', '이병', 2025, 750000, '2025-01-01', '2025-12-31'),
      ('NAVY', '일병', 2025, 900000, '2025-01-01', '2025-12-31'),
      ('NAVY', '상병', 2025, 1200000, '2025-01-01', '2025-12-31'),
      ('NAVY', '병장', 2025, 1500000, '2025-01-01', '2025-12-31'),
      ('AIRFORCE', '이병', 2023, 600000, '2023-01-01', '2023-12-31'),
      ('AIRFORCE', '일병', 2023, 680000, '2023-01-01', '2023-12-31'),
      ('AIRFORCE', '상병', 2023, 800000, '2023-01-01', '2023-12-31'),
      ('AIRFORCE', '병장', 2023, 1000000, '2023-01-01', '2023-12-31'),
      ('AIRFORCE', '이병', 2024, 640000, '2024-01-01', '2024-12-31'),
      ('AIRFORCE', '일병', 2024, 800000, '2024-01-01', '2024-12-31'),
      ('AIRFORCE', '상병', 2024, 1000000, '2024-01-01', '2024-12-31'),
      ('AIRFORCE', '병장', 2024, 1250000, '2024-01-01', '2024-12-31'),
      ('AIRFORCE', '이병', 2025, 750000, '2025-01-01', '2025-12-31'),
      ('AIRFORCE', '일병', 2025, 900000, '2025-01-01', '2025-12-31'),
      ('AIRFORCE', '상병', 2025, 1200000, '2025-01-01', '2025-12-31'),
      ('AIRFORCE', '병장', 2025, 1500000, '2025-01-01', '2025-12-31'),
      ('MARINE', '이병', 2023, 600000, '2023-01-01', '2023-12-31'),
      ('MARINE', '일병', 2023, 680000, '2023-01-01', '2023-12-31'),
      ('MARINE', '상병', 2023, 800000, '2023-01-01', '2023-12-31'),
      ('MARINE', '병장', 2023, 1000000, '2023-01-01', '2023-12-31'),
      ('MARINE', '이병', 2024, 640000, '2024-01-01', '2024-12-31'),
      ('MARINE', '일병', 2024, 800000, '2024-01-01', '2024-12-31'),
      ('MARINE', '상병', 2024, 1000000, '2024-01-01', '2024-12-31'),
      ('MARINE', '병장', 2024, 1250000, '2024-01-01', '2024-12-31'),
      ('MARINE', '이병', 2025, 750000, '2025-01-01', '2025-12-31'),
      ('MARINE', '일병', 2025, 900000, '2025-01-01', '2025-12-31'),
      ('MARINE', '상병', 2025, 1200000, '2025-01-01', '2025-12-31'),
      ('MARINE', '병장', 2025, 1500000, '2025-01-01', '2025-12-31'),
      ('ARMY', '이병', 2026, 750000, '2026-01-01', NULL),
      ('ARMY', '일병', 2026, 900000, '2026-01-01', NULL),
      ('ARMY', '상병', 2026, 1200000, '2026-01-01', NULL),
      ('ARMY', '병장', 2026, 1500000, '2026-01-01', NULL),
      ('NAVY', '이병', 2026, 750000, '2026-01-01', NULL),
      ('NAVY', '일병', 2026, 900000, '2026-01-01', NULL),
      ('NAVY', '상병', 2026, 1200000, '2026-01-01', NULL),
      ('NAVY', '병장', 2026, 1500000, '2026-01-01', NULL),
      ('AIRFORCE', '이병', 2026, 750000, '2026-01-01', NULL),
      ('AIRFORCE', '일병', 2026, 900000, '2026-01-01', NULL),
      ('AIRFORCE', '상병', 2026, 1200000, '2026-01-01', NULL),
      ('AIRFORCE', '병장', 2026, 1500000, '2026-01-01', NULL),
      ('MARINE', '이병', 2026, 750000, '2026-01-01', NULL),
      ('MARINE', '일병', 2026, 900000, '2026-01-01', NULL),
      ('MARINE', '상병', 2026, 1200000, '2026-01-01', NULL),
      ('MARINE', '병장', 2026, 1500000, '2026-01-01', NULL)
ON DUPLICATE KEY UPDATE
                     monthly_salary = VALUES(monthly_salary),
                     effective_from = VALUES(effective_from),
                     effective_to = VALUES(effective_to);

COMMIT;

-- ============================================================
-- Operational reference seed: sql/seed/badge_policy.sql
-- ============================================================
-- 챌린지 뱃지 등급 운영 기준 데이터입니다.
-- 실행: mysql -u <DB_USER> -p jaedaero < sql/seed/badge_policy.sql
-- 기존 사용자 획득 이력은 보존하고 현재 운영 등급만 활성화합니다.

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;
SET time_zone = '+09:00';
START TRANSACTION;

UPDATE badge
SET is_active = FALSE;

INSERT INTO badge (
    badge_name,
    badge_description,
    mission_type,
    required_completion_count,
    grade,
    is_active
)
VALUES
    ('안정형 브론즈', '안정형 미션 1개 완료', 'SAFE', 1, 'BRONZE', TRUE),
    ('안정형 실버', '안정형 미션 10개 완료', 'SAFE', 10, 'SILVER', TRUE),
    ('안정형 골드', '안정형 미션 50개 완료', 'SAFE', 50, 'GOLD', TRUE),
    ('안정형 플래티넘', '안정형 미션 100개 완료', 'SAFE', 100, 'PLATINUM', TRUE),
    ('안정형 다이아', '안정형 미션 300개 완료', 'SAFE', 300, 'DIAMOND', TRUE),
    ('공격형 브론즈', '공격형 미션 1개 완료', 'AGGRESSIVE', 1, 'BRONZE', TRUE),
    ('공격형 실버', '공격형 미션 10개 완료', 'AGGRESSIVE', 10, 'SILVER', TRUE),
    ('공격형 골드', '공격형 미션 50개 완료', 'AGGRESSIVE', 50, 'GOLD', TRUE),
    ('공격형 플래티넘', '공격형 미션 100개 완료', 'AGGRESSIVE', 100, 'PLATINUM', TRUE),
    ('공격형 다이아', '공격형 미션 300개 완료', 'AGGRESSIVE', 300, 'DIAMOND', TRUE)
ON DUPLICATE KEY UPDATE
                     badge_name = VALUES(badge_name),
                     badge_description = VALUES(badge_description),
                     required_completion_count = VALUES(required_completion_count),
                     is_active = TRUE;

COMMIT;

-- ============================================================
-- Operational reference seed: sql/seed/challenge_mission_mock_data.sql
-- ============================================================
-- 노션 운영 기준에 맞춘 챌린지 미션 마스터 데이터입니다.
-- 운영 기준: 데일리 2개, 성향별 추천 후보 4개씩, 1회 미션 1개, 이벤트 미션 2개.
-- 실행 전 20260813_add_mission_code.sql 마이그레이션이 적용되어 있어야 합니다.
-- 기존 완료 이력은 보존하고, 운영 목록 밖의 미션은 비활성화합니다.

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;
SET time_zone = '+09:00';
START TRANSACTION;

CREATE TEMPORARY TABLE expected_mission (
                                            mission_code     VARCHAR(50) COLLATE utf8mb4_unicode_ci NOT NULL,
                                            mission_type     VARCHAR(10) COLLATE utf8mb4_unicode_ci NULL,
                                            mission_category VARCHAR(20) COLLATE utf8mb4_unicode_ci NOT NULL,
                                            title            VARCHAR(255) COLLATE utf8mb4_unicode_ci NOT NULL,
                                            description      TEXT COLLATE utf8mb4_unicode_ci NULL,
                                            action_type      VARCHAR(50) COLLATE utf8mb4_unicode_ci NOT NULL,
                                            display_order    INT NOT NULL,
                                            trigger_type     VARCHAR(30) COLLATE utf8mb4_unicode_ci NOT NULL,
                                            trigger_value    INT NULL,
                                            event_priority   INT NOT NULL,

                                            PRIMARY KEY (mission_code)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;

INSERT INTO expected_mission (
    mission_code,
    mission_type,
    mission_category,
    title,
    description,
    action_type,
    display_order,
    trigger_type,
    trigger_value,
    event_priority
)
VALUES
    ('DAILY_MARKET_REPORT', NULL, 'DAILY', '오늘의 시장 리포트 보기', '오늘의 AI 시장 리포트를 확인해보세요.', 'VIEW_MARKET_REPORT', 1, 'NONE', NULL, 0),
    ('DAILY_TRANSACTION_HISTORY', NULL, 'DAILY', '오늘의 거래 내역 확인하기', '오늘 발생한 거래 내역을 확인해보세요.', 'VIEW_TRANSACTION_HISTORY', 2, 'NONE', NULL, 0),
    ('SAFE_INVESTMENT_GUIDANCE', 'SAFE', 'RECOMMENDED', '적립식 투자 가이드 확인하기', '나에게 맞는 적립식 투자 가이드를 확인해보세요.', 'VIEW_REBALANCING', 1, 'NONE', NULL, 0),
    ('SAFE_MONTHLY_SPENDING_PATTERN', 'SAFE', 'RECOMMENDED', '이번 달 소비 패턴 점검하기', '이번 달 소비 내역을 확인하고 소비 패턴을 점검해보세요.', 'VIEW_SPENDING_ANALYSIS', 2, 'NONE', NULL, 0),
    ('SAFE_SOLDIER_SAVINGS_STATUS', 'SAFE', 'RECOMMENDED', '장병내일준비적금 현황 확인하기', '장병내일준비적금의 납입 현황을 확인해보세요.', 'VIEW_ASSET_OVERVIEW', 3, 'NONE', NULL, 0),
    ('SAFE_DISCHARGE_GOAL_PROGRESS', 'SAFE', 'RECOMMENDED', '전역 목표 달성률 확인하기', '현재 자산과 전역 목표 달성률을 확인해보세요.', 'VIEW_ASSET_STATUS', 4, 'NONE', NULL, 0),
    ('AGGRESSIVE_REBALANCING', 'AGGRESSIVE', 'RECOMMENDED', '투자 추천 확인하기', 'AI가 제안한 투자 추천 내용을 확인해보세요.', 'VIEW_DEPOSIT_PRODUCT', 1, 'NONE', NULL, 0),
    ('AGGRESSIVE_INVESTMENT_RETURN', 'AGGRESSIVE', 'RECOMMENDED', '투자 자산 수익률 확인하기', '투자 자산의 현재 수익률을 확인해보세요.', 'VIEW_ASSET_OVERVIEW', 2, 'NONE', NULL, 0),
    ('AGGRESSIVE_AI_INVESTMENT_ANALYSIS', 'AGGRESSIVE', 'RECOMMENDED', 'AI 투자 분석 받아보기', 'AI 투자 분석을 받아 자산 전략을 점검해보세요.', 'VIEW_DEPOSIT_PRODUCT', 3, 'NONE', NULL, 0),
    ('AGGRESSIVE_WHAT_IF_COMPARISON', 'AGGRESSIVE', 'RECOMMENDED', 'What-if로 투자 금액 비교하기', 'What-if로 투자 금액별 자산 변화를 비교해보세요.', 'RUN_WHAT_IF_SIMULATION', 4, 'NONE', NULL, 0),
    ('SAFE_WHAT_IF_SIMULATION', 'SAFE', 'ONE_TIME', 'What-if 시뮬레이션 하기', '시뮬레이션으로 자산 변화 목표를 설정해보세요.', 'RUN_WHAT_IF_SIMULATION', 1, 'NONE', NULL, 0),
    ('EVENT_PAYDAY_ASSET_ALLOCATION', NULL, 'EVENT', '월급날 자산 배분 해보기', '월급을 저축·투자·소비 목표에 맞춰 배분해보세요.', 'RUN_WHAT_IF_SIMULATION', 1, 'PAYDAY', 10, 1),
    ('EVENT_LEAVE_BENEFIT_CHECK', NULL, 'EVENT', '휴가 혜택 챙기기', '휴가 혜택을 열어 나에게 맞는 혜택을 하나 이상 확인해보세요.', 'VIEW_LEAVE_BENEFIT', 2, 'LEAVE_SCHEDULED', NULL, 2);

-- 코드를 기준으로 수정하여 노출 순서 변경이 기존 미션 ID의 의미를 바꾸지 않게 합니다.
UPDATE mission m
    INNER JOIN expected_mission e
    ON m.mission_code = e.mission_code
SET m.mission_type = e.mission_type,
    m.mission_category = e.mission_category,
    m.title = e.title,
    m.description = e.description,
    m.action_type = e.action_type,
    m.display_order = e.display_order,
    m.trigger_type = e.trigger_type,
    m.trigger_value = e.trigger_value,
    m.event_priority = e.event_priority,
    m.is_active = TRUE;

-- 운영 미션이 없는 DB에서도 같은 식별 코드로 누락분만 생성합니다.
INSERT INTO mission (
    mission_code,
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
    e.mission_code,
    e.mission_type,
    e.mission_category,
    e.title,
    e.description,
    e.action_type,
    e.display_order,
    e.trigger_type,
    e.trigger_value,
    e.event_priority,
    TRUE
FROM expected_mission e
         LEFT JOIN mission m
                   ON m.mission_code = e.mission_code
WHERE m.mission_id IS NULL;

-- 운영 목록에 없는 기존 미션은 완료 이력을 남긴 채 노출만 중지합니다.
UPDATE mission m
    LEFT JOIN expected_mission e
    ON m.mission_code = e.mission_code
SET m.is_active = FALSE
WHERE e.mission_code IS NULL
  AND m.is_active = TRUE;

DROP TEMPORARY TABLE expected_mission;
COMMIT;

-- ============================================================
-- Operational reference seed: sql/seed/leave_benefits.sql
-- ============================================================
-- 나라사랑카드 3기 혜택을 휴가 혜택 탭(교통·여가·자기계발·숙박·기타)으로 분할한 시드입니다.
-- 하나의 할인 조건을 하나의 혜택 항목으로 저장합니다.
-- 출처: https://www.card-gorilla.com/contents/detail/4126

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;
SET time_zone = '+09:00';

START TRANSACTION;

SET @source_url = 'https://www.card-gorilla.com/contents/detail/4126';
SET @sh_target = '신한 나라사랑카드 체크 보유자';
SET @ibk_target = 'IBK나라사랑카드 체크 보유자';
SET @hana_target = '하나 나라사랑카드 체크 보유자';
SET @sh_notice = 'Life 서비스 전월 이용실적에 따른 월 통합 캐시백 한도 5천원~5만원 적용';
SET @ibk_notice = '파워업 All in One·나라서비스 전월 실적·월 한도·횟수 제한 확인 필요';
SET @hana_basic_notice = 'Basic 서비스 전월 이용실적에 따른 월 통합 할인 한도 5천원~5만원 적용';
SET @hana_desire_notice = 'Desire 서비스 이용 시 하나은행 나라사랑통장 군 급여이체 필요';

-- 같은 출처로 제공한 이전 휴가 혜택 시드만 교체합니다.
DELETE FROM military_benefit
WHERE benefit_context = 'LEAVE'
  AND source_url = @source_url;

INSERT INTO military_benefit (
    benefit_context, category, title, effective_from, effective_to, target_text,
    description, usage_method, precautions, source_url, active_yn, display_order
) VALUES
      -- 교통
      ('LEAVE', 'TRANSPORT', '신한 나라사랑카드', NULL, NULL, @sh_target, '대중교통 20% 캐시백', '대상 대중교통을 나라사랑카드로 결제', @sh_notice, @source_url, TRUE, 10),
      ('LEAVE', 'TRANSPORT', '신한 나라사랑카드', NULL, NULL, @sh_target, '광역교통 10% 캐시백', '대상 광역교통을 나라사랑카드로 결제', @sh_notice, @source_url, TRUE, 11),
      ('LEAVE', 'TRANSPORT', '신한 나라사랑카드', NULL, NULL, @sh_target, '카카오T 택시 10% 캐시백', '카카오T 택시를 나라사랑카드로 결제', @sh_notice, @source_url, TRUE, 12),
      ('LEAVE', 'TRANSPORT', 'IBK 나라사랑카드', NULL, NULL, @ibk_target, '대중교통 20% 할인', '대상 대중교통을 IBK나라사랑카드로 결제', '파워업 All in One 서비스 전월 실적·통합 할인 한도·제외 업종 확인 필요', @source_url, TRUE, 20),
      ('LEAVE', 'TRANSPORT', 'IBK 나라사랑카드', NULL, NULL, @ibk_target, '택시 10% 할인', '대상 택시를 IBK나라사랑카드로 결제', '파워업 All in One 서비스 전월 실적·통합 할인 한도·제외 업종 확인 필요', @source_url, TRUE, 21),
      ('LEAVE', 'TRANSPORT', 'IBK 나라사랑카드', NULL, NULL, @ibk_target, 'KTX·SRT·고속버스 5% 할인', '대상 승차권을 IBK나라사랑카드로 결제', '병 급여이체 조건 충족 시 전월 이용실적 조건 면제 가능, 월 할인 한도 확인 필요', @source_url, TRUE, 30),
      ('LEAVE', 'TRANSPORT', '하나 나라사랑카드', NULL, NULL, @hana_target, '택시 20% 할인', '대상 택시를 하나 나라사랑카드로 결제', @hana_desire_notice, @source_url, TRUE, 40),
      ('LEAVE', 'TRANSPORT', '하나 나라사랑카드', NULL, NULL, @hana_target, '대중교통 20% 할인', '대상 대중교통을 하나 나라사랑카드로 결제', @hana_basic_notice, @source_url, TRUE, 41),
      ('LEAVE', 'TRANSPORT', '하나 나라사랑카드', NULL, NULL, @hana_target, '기차·고속버스·시외버스 5% 할인', '대상 교통수단을 하나 나라사랑카드로 결제', @hana_basic_notice, @source_url, TRUE, 42),

      -- 여가
      ('LEAVE', 'LEISURE', '신한 나라사랑카드', NULL, NULL, @sh_target, 'OTT 10% 캐시백', '대상 OTT 서비스에서 나라사랑카드로 결제', 'OTT Life 서비스 전월 실적별 통합 한도 적용', @source_url, TRUE, 10),
      ('LEAVE', 'LEISURE', '신한 나라사랑카드', NULL, NULL, @sh_target, 'CGV 6천원 할인', 'CGV에서 나라사랑카드로 결제', '영화 적용 조건·할인 한도 확인 필요', @source_url, TRUE, 11),
      ('LEAVE', 'LEISURE', '신한 나라사랑카드', NULL, NULL, @sh_target, '테마파크 최대 50% 할인', '대상 테마파크에서 나라사랑카드로 결제', '테마파크 적용 조건·할인 한도 확인 필요', @source_url, TRUE, 12),
      ('LEAVE', 'LEISURE', '신한 나라사랑카드', NULL, NULL, @sh_target, '군 체력단련장·휴양시설 20% 캐시백', '대상 시설을 나라사랑카드로 결제', '슈퍼쏠저 서비스 월 캐시백 한도·적용 대상 시설 확인 필요', @source_url, TRUE, 20),
      ('LEAVE', 'LEISURE', 'IBK 나라사랑카드', NULL, NULL, @ibk_target, '구독·게임·OTT 할인', '대상 서비스에서 IBK나라사랑카드로 결제', @ibk_notice, @source_url, TRUE, 30),
      ('LEAVE', 'LEISURE', 'IBK 나라사랑카드', NULL, NULL, @ibk_target, 'CGV 할인', 'CGV에서 IBK나라사랑카드로 결제', @ibk_notice, @source_url, TRUE, 31),
      ('LEAVE', 'LEISURE', 'IBK 나라사랑카드', NULL, NULL, @ibk_target, '놀이공원 할인', '대상 놀이공원에서 IBK나라사랑카드로 결제', @ibk_notice, @source_url, TRUE, 32),
      ('LEAVE', 'LEISURE', 'IBK 나라사랑카드', NULL, NULL, @ibk_target, 'PC방 할인', '대상 PC방에서 IBK나라사랑카드로 결제', @ibk_notice, @source_url, TRUE, 33),
      ('LEAVE', 'LEISURE', '하나 나라사랑카드', NULL, NULL, @hana_target, '배달앱 20% 할인', '대상 배달앱에서 하나 나라사랑카드로 결제', @hana_desire_notice, @source_url, TRUE, 40),
      ('LEAVE', 'LEISURE', '하나 나라사랑카드', NULL, NULL, @hana_target, 'OTT·앱스토어·멤버십 10% 할인', '대상 서비스에서 하나 나라사랑카드로 결제', @hana_desire_notice, @source_url, TRUE, 41),
      ('LEAVE', 'LEISURE', '하나 나라사랑카드', NULL, NULL, @hana_target, 'CGV 팝콘 스몰세트', 'CGV에서 하나 나라사랑카드로 결제', @hana_desire_notice, @source_url, TRUE, 42),
      ('LEAVE', 'LEISURE', '하나 나라사랑카드', NULL, NULL, @hana_target, '스타벅스·레스토랑 20% 할인', '대상 가맹점에서 하나 나라사랑카드로 결제', @hana_basic_notice, @source_url, TRUE, 50),
      ('LEAVE', 'LEISURE', '하나 나라사랑카드', NULL, NULL, @hana_target, '패스트푸드 5% 할인', '대상 가맹점에서 하나 나라사랑카드로 결제', @hana_basic_notice, @source_url, TRUE, 51),
      ('LEAVE', 'LEISURE', '하나 나라사랑카드', NULL, NULL, @hana_target, '놀이공원 50% 할인', '대상 놀이공원에서 하나 나라사랑카드로 결제', @hana_basic_notice, @source_url, TRUE, 52),

      -- 자기계발
      ('LEAVE', 'SELF_DEVELOPMENT', '신한 나라사랑카드', NULL, NULL, @sh_target, '도서 5% 캐시백', '대상 도서 가맹점에서 나라사랑카드로 결제', @sh_notice, @source_url, TRUE, 10),
      ('LEAVE', 'SELF_DEVELOPMENT', 'IBK 나라사랑카드', NULL, NULL, @ibk_target, '어학시험·교보문고 2천원 할인', '대상 시험 또는 가맹점에서 IBK나라사랑카드로 결제', '병 급여이체 조건 충족 시 전월 이용실적 조건 면제 가능, 세부 조건 확인 필요', @source_url, TRUE, 20),
      ('LEAVE', 'SELF_DEVELOPMENT', '하나 나라사랑카드', NULL, NULL, @hana_target, '어학시험·서점 5% 할인', '대상 시험 또는 서점에서 하나 나라사랑카드로 결제', @hana_basic_notice, @source_url, TRUE, 30),

      -- 숙박
      ('LEAVE', 'LODGING', 'IBK 나라사랑카드', NULL, NULL, @ibk_target, '야놀자 10% 할인', '야놀자에서 IBK나라사랑카드로 결제', '파워업 All in One 서비스 전월 실적·통합 할인 한도·제외 조건 확인 필요', @source_url, TRUE, 10),
      ('LEAVE', 'LODGING', '하나 나라사랑카드', NULL, NULL, @hana_target, '국군콘도 20% 할인', '대상 국군콘도를 하나 나라사랑카드로 결제', @hana_basic_notice, @source_url, TRUE, 20),

      -- 기타
      ('LEAVE', 'ETC', '신한 나라사랑카드', NULL, NULL, @sh_target, 'PX·해군마트 20% 캐시백', 'PX 또는 해군마트에서 나라사랑카드로 결제', '월 최대 10만원 캐시백 한도 적용, 결제금액별 일·월 횟수 제한 확인 필요', @source_url, TRUE, 10),
      ('LEAVE', 'ETC', 'IBK 나라사랑카드', NULL, NULL, @ibk_target, 'PX·해군마트 기본·특별할인 합산 최대 50% 할인', 'PX 또는 해군마트에서 IBK나라사랑카드로 결제', '전월 실적 조건 없음, 현역병·병 급여이체자·결제금액별 할인율·횟수·월 한도 확인 필요', @source_url, TRUE, 20),
      ('LEAVE', 'ETC', '하나 나라사랑카드', NULL, NULL, @hana_target, 'PX 최대 30% 할인', '대상 PX에서 하나 나라사랑카드로 결제', 'Easy 서비스 전월 실적 조건 없음, PX 결제금액별 할인율·월 한도 확인 필요', @source_url, TRUE, 30),
      ('LEAVE', 'ETC', '하나 나라사랑카드', NULL, NULL, @hana_target, '쿠팡·네이버플러스 스토어 20% 할인', '대상 가맹점에서 하나 나라사랑카드로 결제', 'Easy 서비스 전월 실적 조건 없음, 대상 가맹점·할인 한도 확인 필요', @source_url, TRUE, 31),
      ('LEAVE', 'ETC', '하나 나라사랑카드', NULL, NULL, @hana_target, '편의점 행사품목 10% 할인', '대상 편의점에서 하나 나라사랑카드로 결제', 'Easy 서비스 전월 실적 조건 없음, 행사 품목 조건 확인 필요', @source_url, TRUE, 32),
      ('LEAVE', 'ETC', 'IBK 나라사랑카드', NULL, NULL, @ibk_target, '네이버페이 10% 포인트 적립', 'IBK나라사랑카드를 네이버페이에 등록 후 국내 가맹점에서 결제', '월 5회, 최대 5천원 적립 한도 적용', @source_url, TRUE, 40),
      ('LEAVE', 'ETC', 'IBK 나라사랑카드', NULL, NULL, @ibk_target, '해외 결제·ATM 인출 수수료 면제', '해외 결제 또는 해외 ATM 이용 시 IBK나라사랑카드 사용', '전월 실적 조건 없음, 해외 ATM 인출수수료 면제 월 3회 적용', @source_url, TRUE, 50)
ON DUPLICATE KEY UPDATE
                     target_text = VALUES(target_text),
                     description = VALUES(description),
                     usage_method = VALUES(usage_method),
                     precautions = VALUES(precautions),
                     source_url = VALUES(source_url),
                     active_yn = VALUES(active_yn),
                     display_order = VALUES(display_order),
                     updated_at = CURRENT_TIMESTAMP;

UPDATE military_benefit
SET discount_summary = description
WHERE benefit_context = 'LEAVE'
  AND source_url = @source_url;

COMMIT;
