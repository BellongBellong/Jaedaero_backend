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
