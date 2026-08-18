-- 기준 스키마의 military_benefit 단일 테이블에 휴가 혜택 전용 상세 필드를 확장한다.
ALTER TABLE military_benefit
    ADD COLUMN discount_summary VARCHAR(500) NOT NULL DEFAULT '' COMMENT '카드 목록용 할인 요약' AFTER title,
    ADD COLUMN target_text VARCHAR(500) NULL COMMENT '대상 안내' AFTER target_service,
    ADD COLUMN usage_method TEXT NULL COMMENT '이용 방법' AFTER description,
    ADD COLUMN precautions TEXT NULL COMMENT '유의사항(전월 실적·월 한도·횟수 제한·급여이체 조건 등)' AFTER usage_method,
    ADD COLUMN detail_image_url VARCHAR(1000) NULL COMMENT '상세 안내 이미지 URL' AFTER precautions,
    ADD COLUMN active_yn BOOLEAN NOT NULL DEFAULT TRUE COMMENT '노출 여부' AFTER effective_to,
    ADD COLUMN display_order INT NOT NULL DEFAULT 0 COMMENT '카테고리 내 노출 순서' AFTER active_yn,
    ADD CONSTRAINT uq_military_benefit_context_title_category UNIQUE (benefit_context, title, category),
    ADD INDEX idx_military_benefit_leave_lookup (benefit_context, active_yn, category, effective_from, effective_to);
