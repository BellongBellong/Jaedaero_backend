-- 과거 동기화에서 CODEF 표시 계좌번호가 원문으로 저장된 경우를 재마스킹한다.
-- 응답 단계에서도 동일 마스킹을 적용하므로 이 변경 전 데이터도 외부로 노출되지 않는다.
UPDATE connected_account
SET account_masked = CONCAT(
    LEFT(REGEXP_REPLACE(account_masked, '[^0-9A-Za-z]', ''), 6),
    '-**-****',
    RIGHT(REGEXP_REPLACE(account_masked, '[^0-9A-Za-z]', ''), 2)
)
WHERE account_masked IS NOT NULL
  AND CHAR_LENGTH(REGEXP_REPLACE(account_masked, '[^0-9A-Za-z]', '')) > 8;
