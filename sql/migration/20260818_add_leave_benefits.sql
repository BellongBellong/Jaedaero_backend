CREATE TABLE leave_benefit (
    benefit_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '휴가 혜택 ID',
    category ENUM('TRANSPORT', 'LEISURE', 'SELF_DEVELOPMENT', 'LODGING', 'ETC') NOT NULL COMMENT '휴가 혜택 카테고리',
    title VARCHAR(200) NOT NULL COMMENT '혜택명',
    period_start DATE NULL COMMENT '혜택 시작일',
    period_end DATE NULL COMMENT '혜택 종료일',
    target_text VARCHAR(500) NULL COMMENT '대상 안내',
    content TEXT NOT NULL COMMENT '혜택 내용',
    usage_method TEXT NULL COMMENT '이용 방법',
    remark_text TEXT NULL COMMENT '비고(전월 실적·월 한도·횟수 제한·급여이체 조건 등)',
    detail_image_url VARCHAR(1000) NULL COMMENT '상세 안내 이미지 URL',
    source_url VARCHAR(1000) NULL COMMENT '공식 출처 URL',
    active_yn BOOLEAN NOT NULL DEFAULT TRUE COMMENT '노출 여부',
    display_order INT NOT NULL DEFAULT 0 COMMENT '카테고리 내 노출 순서',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT chk_leave_benefit_period CHECK (period_end IS NULL OR period_start IS NULL OR period_start <= period_end),
    INDEX idx_leave_benefit_lookup (active_yn, category, period_start, period_end)
) COMMENT='휴가 중 이용 가능한 군인 혜택'
  DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
