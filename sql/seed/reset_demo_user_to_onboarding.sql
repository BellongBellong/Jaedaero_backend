-- 시연 사용자 온보딩 초기화
-- 소셜 로그인 사용자 행은 보존하고, 온보딩 이후 생성되는 사용자 데이터를 정리합니다.

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;
SET @demo_user_id = 1548561;
START TRANSACTION;

DELETE month_row
FROM cashflow_forecast_month month_row
JOIN cashflow_forecast forecast ON forecast.forecast_id = month_row.forecast_id
WHERE forecast.user_id = @demo_user_id;
DELETE FROM cashflow_forecast WHERE user_id = @demo_user_id;

DELETE FROM ai_recommended_scenario WHERE user_id = @demo_user_id;
DELETE FROM ai_analysis WHERE user_id = @demo_user_id;
DELETE FROM simulation WHERE user_id = @demo_user_id;
DELETE FROM strategy_application WHERE user_id = @demo_user_id;
DELETE FROM investment_guidance WHERE user_id = @demo_user_id;
DELETE FROM discharge_report WHERE user_id = @demo_user_id;
DELETE FROM asset_snapshot WHERE user_id = @demo_user_id;
DELETE FROM leave_mode WHERE user_id = @demo_user_id;
DELETE FROM recurring_investment_plan WHERE user_id = @demo_user_id;
DELETE FROM product_recommendation WHERE user_id = @demo_user_id;

DELETE completion_row
FROM user_mission_completion completion_row
WHERE completion_row.user_id = @demo_user_id;
DELETE FROM challenge_member WHERE user_id = @demo_user_id;
DELETE FROM user_badge WHERE user_id = @demo_user_id;
DELETE FROM investment_badge WHERE user_id = @demo_user_id;

DELETE sync_row
FROM account_transaction_sync sync_row
JOIN connected_account account ON account.account_id = sync_row.account_id
JOIN codef_connection connection ON connection.connection_id = account.connection_id
WHERE connection.user_id = @demo_user_id;
DELETE history
FROM transaction_history history
JOIN connected_account account ON account.account_id = history.account_id
JOIN codef_connection connection ON connection.connection_id = account.connection_id
WHERE connection.user_id = @demo_user_id;
DELETE FROM soldier_saving WHERE user_id = @demo_user_id;
DELETE FROM recurring_investment_plan WHERE user_id = @demo_user_id;
DELETE institution_connection
FROM codef_institution_connection institution_connection
JOIN codef_connection connection ON connection.connection_id = institution_connection.connection_id
WHERE connection.user_id = @demo_user_id;
DELETE account
FROM connected_account account
JOIN codef_connection connection ON connection.connection_id = account.connection_id
WHERE connection.user_id = @demo_user_id;
DELETE FROM codef_connection WHERE user_id = @demo_user_id;

DELETE FROM notification_history WHERE user_id = @demo_user_id;
DELETE FROM device_token WHERE user_id = @demo_user_id;
DELETE FROM refresh_token WHERE user_id = @demo_user_id;
DELETE FROM goal WHERE user_id = @demo_user_id;
DELETE FROM soldier_profile WHERE user_id = @demo_user_id;

COMMIT;
