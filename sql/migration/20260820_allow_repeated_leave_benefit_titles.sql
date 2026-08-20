-- 하나의 카드에 여러 혜택 항목을 둘 수 있도록 휴가 혜택 제목 중복을 허용한다.
-- 동일 카테고리 안에서는 노출 순서까지 포함해 시드 항목을 식별한다.
ALTER TABLE military_benefit
    DROP INDEX uq_military_benefit_context_title_category,
    ADD CONSTRAINT uq_military_benefit_context_category_title_order
        UNIQUE (benefit_context, category, title, display_order);

-- 기존 묶음 항목은 첫 번째 세부 혜택 항목으로 전환해, 시드 재실행 시 갱신되도록 한다.
UPDATE military_benefit
SET title = '신한 나라사랑카드'
WHERE benefit_context = 'LEAVE'
  AND category = 'TRANSPORT'
  AND title = '신한 나라사랑카드 - 대중교통·카카오T 캐시백'
  AND display_order = 10;
