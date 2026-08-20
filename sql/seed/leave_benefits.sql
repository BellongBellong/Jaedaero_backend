-- 나라사랑카드 3기 혜택을 휴가 혜택 탭(교통·여가·자기계발·숙박·기타)으로 분할한 시드입니다.
-- 출처: https://www.card-gorilla.com/contents/detail/4126
-- 카드 혜택·한도·전월 실적은 변경될 수 있으므로 앱에서는 공식 카드사 안내를 함께 확인해야 합니다.

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;
SET time_zone = '+09:00';

START TRANSACTION;

INSERT INTO military_benefit (
    benefit_context, category, title, effective_from, effective_to, target_text,
    description, usage_method, precautions, source_url, active_yn, display_order
) VALUES
    (
        'LEAVE', 'TRANSPORT', '신한 나라사랑카드 - 대중교통·카카오T 캐시백', NULL, NULL, '신한 나라사랑카드 체크 보유자',
        '대중교통 20%, 광역교통 10%, 카카오T 택시 10% 캐시백',
        '대상 교통수단 또는 카카오T 택시를 나라사랑카드로 결제',
        'Life 서비스 전월 이용실적에 따른 월 통합 캐시백 한도 5천원~5만원 적용',
        'https://www.card-gorilla.com/contents/detail/4126', TRUE, 10
    ),
    (
        'LEAVE', 'TRANSPORT', 'IBK 나라사랑카드 - 대중교통·택시 할인', NULL, NULL, 'IBK나라사랑카드 체크 보유자',
        '대중교통 20%, 택시 10% 할인',
        '대상 교통수단 또는 택시를 IBK나라사랑카드로 결제',
        '파워업 All in One 서비스 전월 실적·통합 할인 한도·제외 업종 확인 필요',
        'https://www.card-gorilla.com/contents/detail/4126', TRUE, 20
    ),
    (
        'LEAVE', 'TRANSPORT', 'IBK 나라사랑카드 - KTX·SRT·고속버스 할인', NULL, NULL, 'IBK나라사랑카드 체크 보유자',
        'KTX·SRT·고속버스 5% 할인',
        '대상 승차권을 IBK나라사랑카드로 결제',
        '병 급여이체 조건 충족 시 전월 이용실적 조건 면제 가능, 월 할인 한도 확인 필요',
        'https://www.card-gorilla.com/contents/detail/4126', TRUE, 30
    ),
    (
        'LEAVE', 'TRANSPORT', '하나 나라사랑카드 - 택시 할인', NULL, NULL, '하나 나라사랑카드 체크 보유자',
        '택시 20% 할인',
        '대상 택시를 하나 나라사랑카드로 결제',
        'Desire 서비스 이용 시 하나은행 나라사랑통장 군 급여이체 필요',
        'https://www.card-gorilla.com/contents/detail/4126', TRUE, 40
    ),
    (
        'LEAVE', 'TRANSPORT', '하나 나라사랑카드 - 대중교통·버스 할인', NULL, NULL, '하나 나라사랑카드 체크 보유자',
        '대중교통 20%, 기차·고속버스·시외버스 5% 할인',
        '대상 교통수단을 하나 나라사랑카드로 결제',
        'Basic 서비스 전월 이용실적에 따른 월 통합 할인 한도 5천원~5만원 적용',
        'https://www.card-gorilla.com/contents/detail/4126', TRUE, 50
    ),
    (
        'LEAVE', 'LEISURE', '신한 나라사랑카드 - OTT·영화·테마파크 혜택', NULL, NULL, '신한 나라사랑카드 체크 보유자',
        'OTT 10% 캐시백, CGV 6천원, 테마파크 최대 50% 할인',
        '대상 가맹점 또는 서비스에서 나라사랑카드로 결제',
        'OTT Life 서비스 전월 실적별 통합 한도 적용, 영화·테마파크 조건 확인 필요',
        'https://www.card-gorilla.com/contents/detail/4126', TRUE, 10
    ),
    (
        'LEAVE', 'LEISURE', '신한 나라사랑카드 - 군 휴양시설·스포츠 캐시백', NULL, NULL, '신한 나라사랑카드 체크 보유자',
        '군 체력단련장·휴양시설 20% 캐시백',
        '대상 시설을 나라사랑카드로 결제',
        '슈퍼쏠저 서비스 월 캐시백 한도·적용 대상 시설 확인 필요',
        'https://www.card-gorilla.com/contents/detail/4126', TRUE, 20
    ),
    (
        'LEAVE', 'LEISURE', 'IBK 나라사랑카드 - 구독·게임·OTT·문화 할인', NULL, NULL, 'IBK나라사랑카드 체크 보유자',
        '구독·게임·OTT, CGV, 놀이공원, PC방 할인',
        '대상 가맹점 또는 서비스에서 IBK나라사랑카드로 결제',
        '파워업 All in One·나라서비스 전월 실적·월 한도·횟수 제한 확인 필요',
        'https://www.card-gorilla.com/contents/detail/4126', TRUE, 30
    ),
    (
        'LEAVE', 'LEISURE', '하나 나라사랑카드 - 배달·OTT·영화 혜택', NULL, NULL, '하나 나라사랑카드 체크 보유자',
        '배달앱 20%, OTT·앱스토어·멤버십 10% 할인, CGV 팝콘 스몰세트',
        '대상 서비스에서 하나 나라사랑카드로 결제',
        'Desire 서비스 이용 시 하나은행 나라사랑통장 군 급여이체 필요',
        'https://www.card-gorilla.com/contents/detail/4126', TRUE, 40
    ),
    (
        'LEAVE', 'LEISURE', '하나 나라사랑카드 - 카페·외식·놀이공원 할인', NULL, NULL, '하나 나라사랑카드 체크 보유자',
        '스타벅스·레스토랑 20%, 패스트푸드 5%, 놀이공원 50% 할인',
        '대상 가맹점에서 하나 나라사랑카드로 결제',
        'Basic 서비스 전월 이용실적에 따른 월 통합 할인 한도 5천원~5만원 적용',
        'https://www.card-gorilla.com/contents/detail/4126', TRUE, 50
    ),
    (
        'LEAVE', 'SELF_DEVELOPMENT', '신한 나라사랑카드 - 도서 할인', NULL, NULL, '신한 나라사랑카드 체크 보유자',
        '도서 5% 캐시백',
        '대상 도서 가맹점에서 나라사랑카드로 결제',
        'Life 서비스 전월 이용실적에 따른 월 통합 캐시백 한도 5천원~5만원 적용',
        'https://www.card-gorilla.com/contents/detail/4126', TRUE, 10
    ),
    (
        'LEAVE', 'SELF_DEVELOPMENT', 'IBK 나라사랑카드 - 어학시험·교보문고 할인', NULL, NULL, 'IBK나라사랑카드 체크 보유자',
        '어학시험·교보문고 2천원 할인',
        '대상 시험 또는 가맹점에서 IBK나라사랑카드로 결제',
        '병 급여이체 조건 충족 시 전월 이용실적 조건 면제 가능, 세부 조건 확인 필요',
        'https://www.card-gorilla.com/contents/detail/4126', TRUE, 20
    ),
    (
        'LEAVE', 'SELF_DEVELOPMENT', '하나 나라사랑카드 - 어학시험·서점 할인', NULL, NULL, '하나 나라사랑카드 체크 보유자',
        '어학시험·서점 5% 할인',
        '대상 시험 또는 서점에서 하나 나라사랑카드로 결제',
        'Basic 서비스 전월 이용실적에 따른 월 통합 할인 한도 5천원~5만원 적용',
        'https://www.card-gorilla.com/contents/detail/4126', TRUE, 30
    ),
    (
        'LEAVE', 'LODGING', 'IBK 나라사랑카드 - 야놀자 할인', NULL, NULL, 'IBK나라사랑카드 체크 보유자',
        '야놀자 10% 할인',
        '야놀자에서 IBK나라사랑카드로 결제',
        '파워업 All in One 서비스 전월 실적·통합 할인 한도·제외 조건 확인 필요',
        'https://www.card-gorilla.com/contents/detail/4126', TRUE, 10
    ),
    (
        'LEAVE', 'LODGING', '하나 나라사랑카드 - 국군콘도 할인', NULL, NULL, '하나 나라사랑카드 체크 보유자',
        '국군콘도 20% 할인',
        '대상 국군콘도를 하나 나라사랑카드로 결제',
        'Basic 서비스 전월 이용실적에 따른 월 통합 할인 한도 5천원~5만원 적용',
        'https://www.card-gorilla.com/contents/detail/4126', TRUE, 20
    ),
    (
        'LEAVE', 'ETC', '신한 나라사랑카드 - PX·해군마트 캐시백', NULL, NULL, '신한 나라사랑카드 체크 보유자',
        'PX·해군마트 20% 캐시백',
        'PX 또는 해군마트에서 나라사랑카드로 결제',
        '월 최대 10만원 캐시백 한도 적용, 결제금액별 일·월 횟수 제한 확인 필요',
        'https://www.card-gorilla.com/contents/detail/4126', TRUE, 10
    ),
    (
        'LEAVE', 'ETC', 'IBK 나라사랑카드 - PX 최대 할인', NULL, NULL, 'IBK나라사랑카드 체크 보유자',
        'PX·해군마트 기본·특별할인 합산 최대 50% 할인',
        'PX 또는 해군마트에서 IBK나라사랑카드로 결제',
        '전월 실적 조건 없음, 현역병·병 급여이체자·결제금액별 할인율·횟수·월 한도 확인 필요',
        'https://www.card-gorilla.com/contents/detail/4126', TRUE, 20
    ),
    (
        'LEAVE', 'ETC', '하나 나라사랑카드 - PX·편의점·온라인쇼핑 할인', NULL, NULL, '하나 나라사랑카드 체크 보유자',
        'PX 최대 30%, 쿠팡·네이버플러스 스토어 20%, 편의점 행사품목 10% 할인',
        '대상 가맹점에서 하나 나라사랑카드로 결제',
        'Easy 서비스 전월 실적 조건 없음, PX 결제금액별 할인율·월 한도·편의점 행사 품목 조건 확인 필요',
        'https://www.card-gorilla.com/contents/detail/4126', TRUE, 30
    ),
    (
        'LEAVE', 'ETC', 'IBK 나라사랑카드 - 네이버페이 포인트 적립', NULL, NULL, 'IBK나라사랑카드 체크 보유자',
        '네이버페이 10% 포인트 적립',
        'IBK나라사랑카드를 네이버페이에 등록 후 국내 가맹점에서 결제',
        '월 5회, 최대 5천원 적립 한도 적용',
        'https://www.card-gorilla.com/contents/detail/4126', TRUE, 40
    ),
    (
        'LEAVE', 'ETC', 'IBK 나라사랑카드 - 해외 결제·ATM 수수료 면제', NULL, NULL, 'IBK나라사랑카드 체크 보유자',
        '해외 결제·ATM 인출 수수료 면제',
        '해외 결제 또는 해외 ATM 이용 시 IBK나라사랑카드 사용',
        '전월 실적 조건 없음, 해외 ATM 인출수수료 면제 월 3회 적용',
        'https://www.card-gorilla.com/contents/detail/4126', TRUE, 50
    )
ON DUPLICATE KEY UPDATE
    target_text = VALUES(target_text),
    description = VALUES(description),
    usage_method = VALUES(usage_method),
    precautions = VALUES(precautions),
    source_url = VALUES(source_url),
    active_yn = VALUES(active_yn),
    display_order = VALUES(display_order),
    updated_at = CURRENT_TIMESTAMP;

-- 카드 목록에는 짧은 할인 요약을 사용한다. 상세 조건과 실적·한도는 description과 precautions에 유지한다.
UPDATE military_benefit
SET discount_summary = description
WHERE benefit_context = 'LEAVE'
  AND source_url = 'https://www.card-gorilla.com/contents/detail/4126';

COMMIT;
