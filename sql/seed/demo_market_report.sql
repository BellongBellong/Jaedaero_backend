-- 시연용 오늘의 AI 시장 리포트 시드
-- 기존 사용자·계좌 데이터는 변경하지 않고, 오늘 노출되는 공통 리포트 1건과 지표·출처만 갱신합니다.

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;
SET time_zone = '+09:00';
START TRANSACTION;

INSERT INTO daily_market_report (
    report_date, title, summary, content, report_status, generation_source,
    model_name, prompt_version, valid_from, valid_until
) VALUES (
    CURDATE(),
    '오늘의 AI 시장 리포트',
    '코스피와 코스닥이 상승세를 보인 가운데 원자재 가격 강세와 7월 인플레이션 지표에 시장의 관심이 집중되고 있습니다.',
    '국내 증시에서는 삼성전자가 약 3.6% 상승하면서 코스피가 2일 연속 상승세를 나타냈습니다. 유가와 금 가격은 높은 수준을 유지하고 있으며, 시장은 향후 발표될 7월 인플레이션 데이터와 연준의 9월 통화정책 회의에 주목하고 있습니다.',
    'NORMAL', 'GEMINI', 'demo-market-report-seed', 'demo-market-report-v1',
    DATE_SUB(NOW(), INTERVAL 1 MINUTE), DATE_ADD(NOW(), INTERVAL 1 DAY)
) ON DUPLICATE KEY UPDATE
    report_id = LAST_INSERT_ID(report_id),
    title = VALUES(title),
    summary = VALUES(summary),
    content = VALUES(content),
    report_status = VALUES(report_status),
    generation_source = VALUES(generation_source),
    model_name = VALUES(model_name),
    prompt_version = VALUES(prompt_version),
    valid_from = VALUES(valid_from),
    valid_until = VALUES(valid_until);

SET @report_id = LAST_INSERT_ID();

DELETE FROM daily_market_indicator WHERE report_id = @report_id;
DELETE FROM daily_market_report_source WHERE report_id = @report_id;

INSERT INTO daily_market_indicator (
    report_id, indicator_type, data_as_of, source, observed_value, change_value, change_rate, status
) VALUES
    (@report_id, 'KOSPI', NOW(), '시연용 KRX 데이터', 6299.66, 40.89, 0.65, 'NORMAL'),
    (@report_id, 'KOSDAQ', NOW(), '시연용 KRX 데이터', 854.47, 55.66, 6.97, 'NORMAL'),
    (@report_id, 'US_TREASURY_10Y', NOW(), '시연용 FRED 데이터', 4.6500, -0.0400, -0.85, 'NORMAL'),
    (@report_id, 'USD_KRW', NOW(), '시연용 환율 데이터', 1415.30, NULL, NULL, 'MISSING');

INSERT INTO daily_market_report_source (report_id, source_order, title, url) VALUES
    (@report_id, 1, 'Oil and gold stay near highs - InvestingLive', 'https://investinglive.com/news/investinglive-asia-pacific-financial-market-news-oil-and-gold-stay-near-highs/'),
    (@report_id, 2, 'Gold rises for third straight session - Reuters', 'https://news.google.com/rss/articles/CBMipwFBVV95cUxOQ3pfajk5NldmMDliSlhNU3ZYd1J6UGt3cWM4ZHBzTkxnYTVGbnVWU0ZOVFFqLUk2eFYyQnhxdUxiUFp5Y0lXTnpEOGIwMHBveTR5bzBvZmxjWTA4TmR6OUt0U1ZSd2V0dDhkMlg4TkpFdlFCSEtJN3BXYXJQMFZLZDgyZjR6UTRXeU5fRFZ1MHM1WXpCMEtTOF9CNmptQTJmNzBQN1JyMA?oc=5'),
    (@report_id, 3, 'Oil prices rise, Asia stocks drift - Reuters', 'https://news.google.com/rss/articles/CBMigwFBVV95cUxPRnpVTlc5WDU3ZUFOMTJjSFRLNGFCWUItblV4clc0UnVXUi05VVBaRkFleHpsdWtmcl9YZDRtN3BkaDhqUy1PTWx1aDhjalNDc09RNGh5eDFYN2FnTGxraUlhRWtFUnl0UTJrbV82Yi1CaFEyNmNwaHlvQVdobElUNEZVaw?oc=5'),
    (@report_id, 4, 'What to watch in the week ahead - CNBC', 'https://www.cnbc.com/2026/08/09/here-are-the-2-big-things-were-watching-in-the-stock-market-in-the-week-ahead.html');

COMMIT;
