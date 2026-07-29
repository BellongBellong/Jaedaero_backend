use jaedaero_db;

-- ============================================================
-- JAEDAERO Database Schema
-- MySQL 8.0+
-- ============================================================

-- 기존 스키마를 초기화한 뒤 아래 정의로 다시 생성합니다.
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS investment_badge;
DROP TABLE IF EXISTS refresh_token;
DROP TABLE IF EXISTS discharge_report;
DROP TABLE IF EXISTS strategy_application;
DROP TABLE IF EXISTS ai_recommended_scenario;
DROP TABLE IF EXISTS product_recommendation;
DROP TABLE IF EXISTS military_benefit;
DROP TABLE IF EXISTS financial_product;
DROP TABLE IF EXISTS ai_analysis;
DROP TABLE IF EXISTS leave_budget;
DROP TABLE IF EXISTS simulation;
DROP TABLE IF EXISTS challenge_monthly_result;
DROP TABLE IF EXISTS challenge_member;
DROP TABLE IF EXISTS challenge_group;
DROP TABLE IF EXISTS asset_snapshot;
DROP TABLE IF EXISTS transaction_history;
DROP TABLE IF EXISTS soldier_saving;
DROP TABLE IF EXISTS account_transaction_sync;
DROP TABLE IF EXISTS connected_account;
DROP TABLE IF EXISTS codef_institution_connection;
DROP TABLE IF EXISTS codef_connection;
DROP TABLE IF EXISTS cashflow_forecast_month;
DROP TABLE IF EXISTS cashflow_forecast;
DROP TABLE IF EXISTS military_pay_policy;
DROP TABLE IF EXISTS goal;
DROP TABLE IF EXISTS financial_test;
DROP TABLE IF EXISTS user_agreement;
DROP TABLE IF EXISTS soldier_profile;
DROP TABLE IF EXISTS users;

SET FOREIGN_KEY_CHECKS = 1;

-- ---------------------------------------------
-- 1. users : 사용자
-- ---------------------------------------------
CREATE TABLE users (
                       user_id           BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '사용자 ID',
                       social_type       VARCHAR(20) NOT NULL COMMENT 'KAKAO, GOOGLE',
                       social_id         VARCHAR(255) NOT NULL COMMENT '소셜 제공자 내 사용자 식별자',
                       nickname          VARCHAR(50) NULL COMMENT '닉네임',
                       profile_image     ENUM('ARMY', 'NAVY', 'AIRFORCE', 'MARINE') NULL COMMENT '프로필 아이콘',
                       profile_source    ENUM('GREEN', 'OLIVE', 'YELLOW', 'ORANGE', 'GRAY', 'BLACK') NULL COMMENT '프로필 배경색',
                       military_verified BOOLEAN NOT NULL DEFAULT FALSE COMMENT '군인 인증 완료 여부',
                       is_withdrawn      BOOLEAN NOT NULL DEFAULT FALSE COMMENT '탈퇴 여부',
                       withdrawn_at      TIMESTAMP NULL COMMENT '탈퇴 일시',
                       created_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',
                       updated_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
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
                                 soldier_type    ENUM('ARMY', 'NAVY', 'AIRFORCE', 'MARINE')
        NOT NULL COMMENT '군종',
                                 rank_name       VARCHAR(20) NOT NULL COMMENT '현재 계급',
                                 enlistment_date DATE NOT NULL COMMENT '입대일',
                                 discharge_date  DATE NOT NULL COMMENT '전역 예정일',
                                 saving_join_yn  BOOLEAN NOT NULL DEFAULT FALSE COMMENT '장병내일준비적금 가입 여부',
                                 investment_type ENUM('SAFE', 'BALANCED', 'AGGRESSIVE')
        NULL COMMENT '투자 성향',
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
-- 4. financial_test : 금융 성향 테스트 이력
-- ---------------------------------------------
CREATE TABLE financial_test (
                                test_id      BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '금융 성향 테스트 ID',
                                user_id      BIGINT NOT NULL COMMENT '사용자 ID',
                                score        INT NULL COMMENT '테스트 점수',
                                result_type  ENUM('SAFE', 'BALANCED', 'AGGRESSIVE')
        NULL COMMENT '진단 결과',
                                test_version VARCHAR(20) NULL COMMENT '질문지 버전',
                                created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',

                                CONSTRAINT fk_financial_test_user
                                    FOREIGN KEY (user_id) REFERENCES users(user_id)
                                        ON DELETE CASCADE
) COMMENT='금융 성향 테스트'
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;


-- ---------------------------------------------
-- 5. goal : 전역 자산 목표
-- ---------------------------------------------
CREATE TABLE goal (
                      goal_id       BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '목표 ID',
                      user_id       BIGINT NOT NULL COMMENT '사용자 ID',
                      title         VARCHAR(100) NOT NULL DEFAULT '전역 자산 목표' COMMENT '목표명',
                      goal_type     VARCHAR(50) NULL COMMENT '복학, 취업, 여행, 주거 등',
                      target_amount BIGINT NOT NULL COMMENT '목표 금액',
                      target_date   DATE NULL COMMENT '목표 달성 목표일',
                      status        ENUM('ACTIVE', 'COMPLETED', 'ARCHIVED')
        NOT NULL DEFAULT 'ACTIVE' COMMENT '목표 상태',
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
-- 6. military_pay_policy : 계급별 봉급 정책
-- ---------------------------------------------
CREATE TABLE military_pay_policy (
                                     pay_policy_id  BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '봉급 정책 ID',
                                     soldier_type   ENUM('ARMY', 'NAVY', 'AIRFORCE', 'MARINE')
        NOT NULL COMMENT '군종',
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
) COMMENT='계급별 봉급 정책'
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;


-- ---------------------------------------------
-- 7. cashflow_forecast : 캐시플로우 예측 요약
-- ---------------------------------------------
CREATE TABLE cashflow_forecast (
                                   forecast_id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '예측 결과 ID',
                                   user_id                  BIGINT NOT NULL COMMENT '사용자 ID',
                                   base_asset               BIGINT NOT NULL DEFAULT 0 COMMENT '계산 기준 현재 자산',
                                   expected_salary          BIGINT NOT NULL DEFAULT 0 COMMENT '전역까지 예상 총급여',
                                   expected_saving_amount   BIGINT NOT NULL DEFAULT 0 COMMENT '적금 예상 수령액',
                                   expected_asset           BIGINT NOT NULL DEFAULT 0 COMMENT '전역 예상 자산',
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
-- 8. cashflow_forecast_month : 월별 캐시플로우
-- ---------------------------------------------
CREATE TABLE cashflow_forecast_month (
                                         forecast_month_id       BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '월별 예측 ID',
                                         forecast_id             BIGINT NOT NULL COMMENT '예측 결과 ID',
                                         forecast_month          DATE NOT NULL COMMENT '예측 월의 첫날',
                                         expected_rank           VARCHAR(20) NULL COMMENT '예상 계급',
                                         expected_salary         BIGINT NOT NULL DEFAULT 0 COMMENT '예상 급여',
                                         expected_saving_amount  BIGINT NOT NULL DEFAULT 0 COMMENT '예상 저축액',
                                         expected_spending_amount BIGINT NOT NULL DEFAULT 0 COMMENT '예상 소비액',
                                         expected_ending_asset   BIGINT NOT NULL DEFAULT 0 COMMENT '월말 예상 자산',

                                         CONSTRAINT uq_cashflow_forecast_month
                                             UNIQUE (forecast_id, forecast_month),
                                         CONSTRAINT fk_cashflow_forecast_month_forecast
                                             FOREIGN KEY (forecast_id) REFERENCES cashflow_forecast(forecast_id)
                                                 ON DELETE CASCADE
) COMMENT='월별 캐시플로우 예측'
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;


-- ---------------------------------------------
-- 9. codef_connection : CODEF 금융 연동
-- ---------------------------------------------
CREATE TABLE codef_connection (
                                  connection_id            BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '금융 연동 ID',
                                  user_id                  BIGINT NOT NULL COMMENT '사용자 ID',
                                  connected_id_encrypted   VARCHAR(1024) NOT NULL COMMENT '암호화된 CODEF Connected ID',
                                  connected_id_hash        CHAR(64) NOT NULL COMMENT 'Connected ID SHA-256 해시',
                                  status                   ENUM('ACTIVE', 'DISCONNECTED', 'ERROR')
        NOT NULL DEFAULT 'ACTIVE' COMMENT '연동 상태',
                                  last_sync_at             TIMESTAMP NULL COMMENT '전체 계좌 마지막 동기화 시각',
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
-- 10. codef_institution_connection : Connected ID에 등록된 기관
-- ---------------------------------------------
CREATE TABLE codef_institution_connection (
                                               institution_connection_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'CODEF 기관 연결 ID',
                                               connection_id             BIGINT NOT NULL COMMENT 'CODEF 연동 ID',
                                               institution_code          VARCHAR(20) NOT NULL COMMENT 'CODEF 기관 코드',
                                               business_type             VARCHAR(2) NOT NULL COMMENT 'CODEF 업무 구분(BK: 은행, ST: 증권)',
                                               login_type                VARCHAR(10) NULL COMMENT '등록에 사용한 CODEF 로그인 방식',
                                               status                    ENUM('ACTIVE', 'ERROR', 'DISCONNECTED') NOT NULL DEFAULT 'ACTIVE' COMMENT '기관 등록 상태',
                                               last_sync_at              TIMESTAMP NULL COMMENT '기관 마지막 동기화 시각',
                                               last_sync_error_message   VARCHAR(500) NULL COMMENT '기관 최근 동기화 실패 사유',
                                               created_at                TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',
                                               updated_at                TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 일시',

                                               CONSTRAINT uq_codef_institution_connection
                                                   UNIQUE (connection_id, institution_code, business_type),
                                               CONSTRAINT fk_codef_institution_connection_connection
                                                   FOREIGN KEY (connection_id) REFERENCES codef_connection(connection_id)
                                                       ON DELETE CASCADE
) COMMENT='CODEF Connected ID에 등록된 금융기관'
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;


-- ---------------------------------------------
-- 11. connected_account : CODEF 연동 계좌
-- ---------------------------------------------
CREATE TABLE connected_account (
                                   account_id                BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '연동 계좌 ID',
                                   connection_id             BIGINT NOT NULL COMMENT 'CODEF 연동 ID',
                                   institution_code          VARCHAR(20) NOT NULL COMMENT 'CODEF 기관 코드',
                                   business_type             VARCHAR(2) NOT NULL DEFAULT 'BK' COMMENT 'CODEF 업무 구분(BK: 은행, ST: 증권)',
                                   institution_name          VARCHAR(100) NOT NULL COMMENT '금융기관명',
                                   account_number_encrypted  VARCHAR(1024) NOT NULL COMMENT '암호화된 실제 계좌번호',
                                   account_number_hash       CHAR(64) NOT NULL COMMENT '계좌번호 SHA-256 해시',
                                   account_masked            VARCHAR(50) NOT NULL COMMENT '화면 표시용 마스킹 계좌번호',
                                   account_type              VARCHAR(50) NULL COMMENT '입출금, 적금, 대출, 펀드 등',
                                   account_role              ENUM('SOLDIER_SAVING', 'NARASARANG', 'GENERAL') NOT NULL DEFAULT 'GENERAL' COMMENT '계좌 분류',
                                   product_name              VARCHAR(255) NULL COMMENT '상품명',
                                   current_balance           BIGINT NOT NULL DEFAULT 0 COMMENT '조회 시점 잔액',
                                   available_balance         BIGINT NULL COMMENT '출금 가능 금액',
                                   account_opened_date       DATE NULL COMMENT '계좌 개설일',
                                   maturity_date             DATE NULL COMMENT '만기일',
                                   last_synced_at            TIMESTAMP NULL COMMENT '계좌 마지막 동기화 시각',
                                   status                    ENUM('ACTIVE', 'DISCONNECTED')
        NOT NULL DEFAULT 'ACTIVE' COMMENT '계좌 상태',
                                   created_at                TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',
                                   updated_at                TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                                       ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 일시',

                                   CONSTRAINT uq_connected_account_source
                                       UNIQUE (connection_id, institution_code, business_type, account_number_hash),
                                   CONSTRAINT fk_connected_account_connection
                                       FOREIGN KEY (connection_id) REFERENCES codef_connection(connection_id)
                                           ON DELETE CASCADE
) COMMENT='CODEF 연동 계좌'
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;


-- ---------------------------------------------
-- 10-1. account_transaction_sync : 거래내역 동기화 범위
-- ---------------------------------------------
CREATE TABLE IF NOT EXISTS account_transaction_sync (
                                                        sync_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '거래내역 조회 범위 동기화 ID',
                                                        account_id BIGINT NOT NULL COMMENT '연동 계좌 ID',
                                                        inquiry_type ENUM('DEMAND_DEPOSIT', 'INSTALLMENT_SAVINGS') NOT NULL COMMENT '조회 API 유형',
                                                        requested_start_date DATE NOT NULL COMMENT 'CODEF 조회 시작일',
                                                        requested_end_date DATE NOT NULL COMMENT 'CODEF 조회 종료일',
                                                        synced_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '동기화 완료 시각',
                                                        CONSTRAINT uq_account_transaction_sync_period
                                                            UNIQUE (account_id, inquiry_type, requested_start_date, requested_end_date),
                                                        CONSTRAINT fk_account_transaction_sync_account
                                                            FOREIGN KEY (account_id) REFERENCES connected_account(account_id) ON DELETE CASCADE,
                                                        CONSTRAINT chk_account_transaction_sync_period
                                                            CHECK (requested_start_date <= requested_end_date)
) COMMENT='계좌 거래내역 동기화 범위'
    DEFAULT CHARSET=utf8mb4
    COLLATE=utf8mb4_unicode_ci;


-- ---------------------------------------------
-- 11. soldier_saving : 장병내일준비적금 상세
-- ---------------------------------------------
CREATE TABLE soldier_saving (
                                saving_id                    BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '장병 적금 ID',
                                user_id                      BIGINT NOT NULL COMMENT '사용자 ID',
                                account_id                   BIGINT NULL COMMENT '연동 적금 계좌 ID',
                                source_type                  ENUM('CODEF', 'MANUAL')
        NOT NULL DEFAULT 'CODEF' COMMENT '등록 출처',
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
-- 12. transaction_history : 거래 내역
-- ---------------------------------------------
CREATE TABLE transaction_history (
                                     transaction_id           BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '거래 내역 ID',
                                     account_id               BIGINT NOT NULL COMMENT '연동 계좌 ID',
                                     transaction_datetime     DATETIME NOT NULL COMMENT '거래 일시',
                                     amount                   BIGINT NOT NULL COMMENT '거래 금액',
                                     balance_after            BIGINT NULL COMMENT '거래 후 잔액',
                                     transaction_type         ENUM('DEPOSIT', 'WITHDRAW')
        NOT NULL COMMENT '입금 또는 출금',
                                     category                 VARCHAR(50) NULL COMMENT 'AI 소비 카테고리',
                                     category_source          ENUM('RULE', 'AI', 'USER')
        NULL COMMENT '카테고리 생성 주체',
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
) COMMENT='일별 자산 스냅샷'
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;


-- ---------------------------------------------
-- 14. challenge_group : 입대 동기 챌린지 그룹
-- ---------------------------------------------
CREATE TABLE challenge_group (
                                 group_id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '동기 그룹 ID',
                                 soldier_type      ENUM('ARMY', 'NAVY', 'AIRFORCE', 'MARINE')
        NOT NULL COMMENT '군종',
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
-- 15. challenge_member : 챌린지 참여자
-- ---------------------------------------------
CREATE TABLE challenge_member (
                                  member_id    BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '챌린지 참여 ID',
                                  group_id     BIGINT NOT NULL COMMENT '동기 그룹 ID',
                                  user_id      BIGINT NOT NULL COMMENT '사용자 ID',
                                  joined_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '참여 일시',

                                  CONSTRAINT uq_challenge_member_group_user UNIQUE (group_id, user_id),
                                  CONSTRAINT fk_challenge_member_group
                                      FOREIGN KEY (group_id) REFERENCES challenge_group(group_id)
                                          ON DELETE CASCADE,
                                  CONSTRAINT fk_challenge_member_user
                                      FOREIGN KEY (user_id) REFERENCES users(user_id)
                                          ON DELETE CASCADE
) COMMENT='챌린지 참여자'
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;


-- ---------------------------------------------
-- 16. challenge_monthly_result : 월별 챌린지 결과
-- ---------------------------------------------
CREATE TABLE challenge_monthly_result (
                                          challenge_result_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '월별 챌린지 결과 ID',
                                          member_id           BIGINT NOT NULL COMMENT '챌린지 참여 ID',
                                          result_month        DATE NOT NULL COMMENT '결과 월의 첫날',
                                          saving_rate         DECIMAL(5,2) NOT NULL COMMENT '저축률',
                                          ranking_no          INT NULL COMMENT '동기 그룹 내 순위',
                                          created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',

                                          CONSTRAINT uq_challenge_monthly_result
                                              UNIQUE (member_id, result_month),
                                          CONSTRAINT fk_challenge_monthly_result_member
                                              FOREIGN KEY (member_id) REFERENCES challenge_member(member_id)
                                                  ON DELETE CASCADE,
                                          CONSTRAINT chk_challenge_monthly_result_saving_rate
                                              CHECK (saving_rate BETWEEN 0 AND 100)
) COMMENT='월별 챌린지 결과'
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;


-- ---------------------------------------------
-- 17. simulation : 사용자 What-if 시뮬레이션
-- ---------------------------------------------
CREATE TABLE simulation (
                            simulation_id            BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '시뮬레이션 ID',
                            user_id                  BIGINT NOT NULL COMMENT '사용자 ID',
                            scenario_name            VARCHAR(100) NOT NULL COMMENT '시나리오명',
                            monthly_saving_amount    BIGINT NOT NULL COMMENT '월 저축액',
                            investment_ratio         DECIMAL(5,2) NOT NULL COMMENT '투자 비율',
                            expected_return_rate     DECIMAL(5,2) NULL COMMENT '목표 투자수익률(%, 연 환산)',
                            investment_type          ENUM('SAFE', 'BALANCED', 'AGGRESSIVE')
        NOT NULL COMMENT '투자 성향',
                            monthly_spending_amount  BIGINT NOT NULL COMMENT '월 소비액',
                            expected_asset           BIGINT NOT NULL COMMENT '전역 예상 자산',
                            financial_discharge_date DATE NULL COMMENT '재정적 전역일',
                            is_saved                 BOOLEAN NOT NULL DEFAULT TRUE COMMENT '사용자 저장 여부',
                            created_at               TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',
                            updated_at               TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                                ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 일시',

                            CONSTRAINT fk_simulation_user
                                FOREIGN KEY (user_id) REFERENCES users(user_id)
                                    ON DELETE CASCADE,
                            CONSTRAINT chk_simulation_investment_ratio
                                CHECK (investment_ratio BETWEEN 0 AND 100)
) COMMENT='사용자 What-if 시뮬레이션'
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;


-- ---------------------------------------------
-- 18. leave_budget : 휴가 예산
-- ---------------------------------------------
CREATE TABLE leave_budget (
                              budget_id         BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '휴가 예산 ID',
                              user_id           BIGINT NOT NULL COMMENT '사용자 ID',
                              leave_name        VARCHAR(100) NULL COMMENT '휴가명',
                              budget_amount     BIGINT NULL COMMENT '예산 금액',
                              expected_expense  BIGINT NULL COMMENT '예상 지출 금액',
                              actual_expense    BIGINT NULL COMMENT '실제 지출 금액',
                              leave_start_date  DATE NULL COMMENT '휴가 시작일',
                              leave_end_date    DATE NULL COMMENT '휴가 종료일',
                              status            ENUM('PLANNED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED')
        NOT NULL DEFAULT 'PLANNED' COMMENT '휴가 예산 상태',
                              created_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',
                              updated_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                                  ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 일시',

                              CONSTRAINT fk_leave_budget_user
                                  FOREIGN KEY (user_id) REFERENCES users(user_id)
                                      ON DELETE CASCADE,
                              CONSTRAINT chk_leave_budget_date
                                  CHECK (leave_end_date IS NULL OR leave_start_date IS NULL OR leave_end_date >= leave_start_date)
) COMMENT='휴가 예산'
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;


-- ---------------------------------------------
-- 19. ai_analysis : AI 분석 이력
-- ---------------------------------------------
CREATE TABLE ai_analysis (
                             analysis_id       BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'AI 분석 ID',
                             user_id           BIGINT NOT NULL COMMENT '사용자 ID',
                             snapshot_id       BIGINT NULL COMMENT '분석 기준 자산 스냅샷 ID',
                             simulation_id     BIGINT NULL COMMENT '분석 기준 시뮬레이션 ID',
                             analysis_type     ENUM(
        'CONSUMPTION',
        'SAVING',
        'INVESTMENT',
        'POLICY',
        'DIAGNOSIS',
        'SCENARIO_COMPARISON'
    ) NOT NULL COMMENT '분석 유형',
                             result_json       JSON NOT NULL COMMENT 'AI 분석 결과',
                             model_name        VARCHAR(100) NULL COMMENT 'AI 모델명',
                             prompt_version    VARCHAR(50) NULL COMMENT '프롬프트 버전',
                             created_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',

                             CONSTRAINT fk_ai_analysis_user
                                 FOREIGN KEY (user_id) REFERENCES users(user_id)
                                     ON DELETE CASCADE,
                             CONSTRAINT fk_ai_analysis_snapshot
                                 FOREIGN KEY (snapshot_id) REFERENCES asset_snapshot(snapshot_id)
                                     ON DELETE SET NULL,
                             CONSTRAINT fk_ai_analysis_simulation
                                 FOREIGN KEY (simulation_id) REFERENCES simulation(simulation_id)
                                     ON DELETE SET NULL
) COMMENT='AI 분석 이력'
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;


-- ---------------------------------------------
-- 20. financial_product : 금융 상품
-- ---------------------------------------------
CREATE TABLE financial_product (
                                   product_id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '금융 상품 ID',
                                   product_name        VARCHAR(255) NOT NULL COMMENT '상품명',
                                   company_name        VARCHAR(100) NULL COMMENT '금융회사명',
                                   product_type        VARCHAR(50) NULL COMMENT '상품 유형',
                                   base_interest_rate  DECIMAL(5,2) NULL COMMENT '기본 금리',
                                   max_interest_rate   DECIMAL(5,2) NULL COMMENT '최고 금리',
                                   return_rate_1y      DECIMAL(6,2) NULL COMMENT '최근 1년 수익률(%)',
                                   eligibility         TEXT NULL COMMENT '가입 조건',
                                   description         TEXT NULL COMMENT '상품 설명',
                                   source_url          VARCHAR(1000) NULL COMMENT '출처 URL',
                                   as_of_date          DATE NULL COMMENT '정보 기준일',
                                   status              ENUM('ACTIVE', 'EXPIRED')
        NOT NULL DEFAULT 'ACTIVE' COMMENT '상품 상태',
                                   created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',
                                   updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                                       ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 일시',

                                   CONSTRAINT uq_financial_product UNIQUE (product_name, company_name)
) COMMENT='금융 상품'
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;


-- ---------------------------------------------
-- 21. military_benefit : 군인 혜택 및 청년 정책
-- ---------------------------------------------
CREATE TABLE military_benefit (
                                  benefit_id      BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '혜택 ID',
                                  title           VARCHAR(255) NOT NULL COMMENT '혜택명',
                                  category        VARCHAR(50) NULL COMMENT '혜택 유형',
                                  description     TEXT NULL COMMENT '혜택 설명',
                                  target_rank     VARCHAR(30) NULL COMMENT '대상 계급',
                                  target_service  VARCHAR(30) NULL COMMENT '대상 군종',
                                  source_url      VARCHAR(1000) NULL COMMENT '출처 URL',
                                  effective_from  DATE NULL COMMENT '시행일',
                                  effective_to    DATE NULL COMMENT '종료일',
                                  created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',
                                  updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                                      ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 일시'
) COMMENT='군인 혜택 및 청년 정책'
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;


-- ---------------------------------------------
-- 22. product_recommendation : 금융 상품 추천
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
) COMMENT='금융 상품 추천'
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;


-- ---------------------------------------------
-- 23. ai_recommended_scenario : AI 추천 시나리오
-- ---------------------------------------------
CREATE TABLE ai_recommended_scenario (
                                         scenario_id               BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'AI 추천 시나리오 ID',
                                         user_id                   BIGINT NOT NULL COMMENT '사용자 ID',
                                         monthly_saving_amount     BIGINT NOT NULL COMMENT '추천 월 저축액',
                                         investment_ratio          DECIMAL(5,2) NOT NULL COMMENT '추천 투자 비율',
                                         expected_return_rate      DECIMAL(5,2) NULL COMMENT '목표 투자수익률(%)',
                                         investment_type           ENUM('SAFE', 'BALANCED', 'AGGRESSIVE')
        NOT NULL COMMENT '추천 투자 성향',
                                         monthly_spending_amount   BIGINT NOT NULL COMMENT '추천 월 소비액',
                                         expected_asset            BIGINT NOT NULL COMMENT '추천 전역 예상 자산',
                                         financial_discharge_date  DATE NULL COMMENT '추천 재정적 전역일',
                                         recommend_reason          TEXT NULL COMMENT '추천 사유',
                                         created_at                TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',

                                         CONSTRAINT fk_ai_recommended_scenario_user
                                             FOREIGN KEY (user_id) REFERENCES users(user_id)
                                                 ON DELETE CASCADE,
                                         CONSTRAINT chk_ai_recommended_scenario_investment_ratio
                                             CHECK (investment_ratio BETWEEN 0 AND 100)
) COMMENT='AI 추천 시나리오'
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;


-- ---------------------------------------------
-- 24. strategy_application : 전략 적용 이력
-- ---------------------------------------------
CREATE TABLE strategy_application (
                                      application_id                   BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '전략 적용 ID',
                                      user_id                          BIGINT NOT NULL COMMENT '사용자 ID',
                                      source_type                      ENUM('SIMULATION', 'AI_RECOMMENDATION', 'MANUAL')
        NOT NULL COMMENT '적용 출처',
                                      simulation_id                    BIGINT NULL COMMENT '원본 시뮬레이션 ID',
                                      ai_scenario_id                   BIGINT NULL COMMENT '원본 AI 추천 시나리오 ID',
                                      applied_monthly_saving_amount    BIGINT NULL COMMENT '적용 월 저축액',
                                      applied_investment_ratio         DECIMAL(5,2) NULL COMMENT '적용 투자 비율',
                                      applied_expected_return_rate     DECIMAL(5,2) NULL COMMENT '적용 목표 투자수익률(%)',
                                      applied_investment_type          ENUM('SAFE', 'BALANCED', 'AGGRESSIVE')
        NULL COMMENT '적용 투자 성향',
                                      applied_monthly_spending_amount  BIGINT NULL COMMENT '적용 월 소비액',
                                      before_expected_asset            BIGINT NULL COMMENT '적용 전 예상 자산',
                                      after_expected_asset             BIGINT NULL COMMENT '적용 후 예상 자산',
                                      applied_at                       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '적용 일시',

                                      CONSTRAINT fk_strategy_application_user
                                          FOREIGN KEY (user_id) REFERENCES users(user_id)
                                              ON DELETE CASCADE,
                                      CONSTRAINT fk_strategy_application_simulation
                                          FOREIGN KEY (simulation_id) REFERENCES simulation(simulation_id)
                                              ON DELETE SET NULL,
                                      CONSTRAINT fk_strategy_application_ai_scenario
                                          FOREIGN KEY (ai_scenario_id) REFERENCES ai_recommended_scenario(scenario_id)
                                              ON DELETE SET NULL,
                                      CONSTRAINT chk_strategy_application_investment_ratio
                                          CHECK (
                                              applied_investment_ratio IS NULL
                                                  OR applied_investment_ratio BETWEEN 0 AND 100
                                              )
) COMMENT='전략 적용 이력'
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
) COMMENT='전역 리포트'
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;


-- ---------------------------------------------
-- 26. refresh_token : JWT Refresh Token 관리
-- ---------------------------------------------
CREATE TABLE refresh_token (
                               token_id   BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '토큰 ID',
                               user_id    BIGINT NOT NULL COMMENT '사용자 ID',
                               token_hash CHAR(64) NOT NULL COMMENT 'Refresh Token SHA-256 해시값',
                               user_agent VARCHAR(255) NULL COMMENT '발급 기기 정보',
                               expires_at DATETIME NOT NULL COMMENT '만료 일시',
                               created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',

                               CONSTRAINT uq_refresh_token_hash UNIQUE (token_hash),
                               CONSTRAINT fk_refresh_token_user
                                   FOREIGN KEY (user_id) REFERENCES users(user_id)
                                       ON DELETE CASCADE
) COMMENT='JWT Refresh Token 관리'
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;


-- ---------------------------------------------
-- 27. investment_badge : 투자 뱃지
-- ---------------------------------------------
CREATE TABLE investment_badge (
                                   badge_id            BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '투자 뱃지 ID',
                                   user_id             BIGINT NOT NULL COMMENT '사용자 ID',
                                   badge_month         DATE NOT NULL COMMENT '산정 기준 월의 첫날',
                                   monthly_return_rate DECIMAL(6,2) NOT NULL COMMENT '해당 월 저축+투자 합산 수익률(%)',
                                   badge_tier          ENUM('SAFE', 'BALANCED', 'AGGRESSIVE') NOT NULL COMMENT '월간 뱃지 등급',
                                   badge_grade         ENUM('BRONZE', 'SILVER', 'GOLD', 'PLATINUM', 'DIAMOND') NULL COMMENT '누적 등급',
                                   created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',

                                   CONSTRAINT uq_investment_badge_user_month UNIQUE (user_id, badge_month),
                                   CONSTRAINT fk_investment_badge_user
                                       FOREIGN KEY (user_id) REFERENCES users(user_id)
                                           ON DELETE CASCADE
) COMMENT='투자 뱃지'
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;


-- ---------------------------------------------
-- 데모 기본 사용자
-- ---------------------------------------------
INSERT INTO users (user_id, social_type, social_id, nickname)
VALUES (1, 'DEMO', 'codef-demo-1', 'CODEF 데모 사용자');
