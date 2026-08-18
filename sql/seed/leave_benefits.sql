-- 나라사랑카드 3기 혜택을 휴가 혜택 탭(교통·여가·자기계발·숙박·기타)으로 분할한 시드입니다.
-- 출처: https://www.card-gorilla.com/contents/detail/4126
-- 카드 혜택·한도·전월 실적은 변경될 수 있으므로 앱에서는 공식 카드사 안내를 함께 확인해야 합니다.

START TRANSACTION;

INSERT INTO leave_benefit (
    category, title, period_start, period_end, target_text,
    content, usage_method, remark_text, source_url, active_yn, display_order
) VALUES
    (
        'TRANSPORT', '신한 나라사랑카드 - 대중교통·카카오T 캐시백', NULL, NULL, '신한 나라사랑카드 체크 보유자',
        '대중교통 20%, 광역교통 10%, 카카오T 택시 10% 캐시백을 제공합니다.',
        '대상 교통수단 또는 카카오T 택시를 나라사랑카드로 결제합니다.',
        'Life 서비스 전월 이용실적에 따라 월 통합 캐시백 한도 5천원~5만원이 적용됩니다.',
        'https://www.card-gorilla.com/contents/detail/4126', TRUE, 10
    ),
    (
        'TRANSPORT', 'IBK 나라사랑카드 - 대중교통·택시 할인', NULL, NULL, 'IBK나라사랑카드 체크 보유자',
        '대중교통 20%, 택시 10% 할인을 제공합니다.',
        '대상 교통수단 또는 택시를 IBK나라사랑카드로 결제합니다.',
        '파워업 All in One 서비스의 전월 실적·통합 할인 한도와 제외 업종을 카드사 안내에서 확인해야 합니다.',
        'https://www.card-gorilla.com/contents/detail/4126', TRUE, 20
    ),
    (
        'TRANSPORT', 'IBK 나라사랑카드 - KTX·SRT·고속버스 할인', NULL, NULL, 'IBK나라사랑카드 체크 보유자',
        'KTX·SRT·고속버스 결제 시 5% 할인을 제공합니다.',
        '대상 승차권을 IBK나라사랑카드로 결제합니다.',
        '병 급여이체 조건 충족 시 전월 이용실적 조건이 면제될 수 있습니다. 월 할인 한도는 카드사 안내를 확인해야 합니다.',
        'https://www.card-gorilla.com/contents/detail/4126', TRUE, 30
    ),
    (
        'TRANSPORT', '하나 나라사랑카드 - 택시 할인', NULL, NULL, '하나 나라사랑카드 체크 보유자',
        '택시 결제 시 20% 할인을 제공합니다.',
        '대상 택시 결제를 하나 나라사랑카드로 진행합니다.',
        'Desire 서비스로 하나은행 나라사랑통장 군 급여이체가 필요합니다.',
        'https://www.card-gorilla.com/contents/detail/4126', TRUE, 40
    ),
    (
        'TRANSPORT', '하나 나라사랑카드 - 대중교통·버스 할인', NULL, NULL, '하나 나라사랑카드 체크 보유자',
        '대중교통 20%, 기차·고속버스·시외버스 5% 할인을 제공합니다.',
        '대상 교통수단을 하나 나라사랑카드로 결제합니다.',
        'Basic 서비스 전월 이용실적에 따라 월 통합 할인 한도 5천원~5만원이 적용됩니다.',
        'https://www.card-gorilla.com/contents/detail/4126', TRUE, 50
    ),
    (
        'LEISURE', '신한 나라사랑카드 - OTT·영화·테마파크 혜택', NULL, NULL, '신한 나라사랑카드 체크 보유자',
        'OTT 10% 캐시백, CGV 6천원 제공, 테마파크 최대 50% 할인 혜택을 제공합니다.',
        '대상 가맹점 또는 서비스에서 나라사랑카드로 결제합니다.',
        'OTT는 Life 서비스 전월 실적별 통합 한도가 적용됩니다. 영화·테마파크 적용 조건은 카드사 안내를 확인해야 합니다.',
        'https://www.card-gorilla.com/contents/detail/4126', TRUE, 10
    ),
    (
        'LEISURE', '신한 나라사랑카드 - 군 휴양시설·스포츠 캐시백', NULL, NULL, '신한 나라사랑카드 체크 보유자',
        '군 체력단련장 및 휴양시설 이용 시 20% 캐시백을 제공합니다.',
        '대상 시설을 나라사랑카드로 결제합니다.',
        '슈퍼쏠저 서비스의 월 캐시백 한도와 적용 대상 시설은 카드사 안내를 확인해야 합니다.',
        'https://www.card-gorilla.com/contents/detail/4126', TRUE, 20
    ),
    (
        'LEISURE', 'IBK 나라사랑카드 - 구독·게임·OTT·문화 할인', NULL, NULL, 'IBK나라사랑카드 체크 보유자',
        '구독·게임·OTT, CGV, 놀이공원, PC방 이용 할인 혜택을 제공합니다.',
        '대상 가맹점 또는 서비스에서 IBK나라사랑카드로 결제합니다.',
        '파워업 All in One 및 나라서비스의 전월 실적·월 한도·횟수 제한을 카드사 안내에서 확인해야 합니다.',
        'https://www.card-gorilla.com/contents/detail/4126', TRUE, 30
    ),
    (
        'LEISURE', '하나 나라사랑카드 - 배달·OTT·영화 혜택', NULL, NULL, '하나 나라사랑카드 체크 보유자',
        '배달앱 20%, OTT·앱스토어·멤버십 10% 할인과 CGV 팝콘 스몰세트 무료 혜택을 제공합니다.',
        '대상 서비스에서 하나 나라사랑카드로 결제합니다.',
        'Desire 서비스로 하나은행 나라사랑통장 군 급여이체가 필요합니다.',
        'https://www.card-gorilla.com/contents/detail/4126', TRUE, 40
    ),
    (
        'LEISURE', '하나 나라사랑카드 - 카페·외식·놀이공원 할인', NULL, NULL, '하나 나라사랑카드 체크 보유자',
        '스타벅스·레스토랑 20%, 패스트푸드 5%, 놀이공원 50% 할인 혜택을 제공합니다.',
        '대상 가맹점에서 하나 나라사랑카드로 결제합니다.',
        'Basic 서비스 전월 이용실적에 따라 월 통합 할인 한도 5천원~5만원이 적용됩니다.',
        'https://www.card-gorilla.com/contents/detail/4126', TRUE, 50
    ),
    (
        'SELF_DEVELOPMENT', '신한 나라사랑카드 - 도서 할인', NULL, NULL, '신한 나라사랑카드 체크 보유자',
        '도서 결제 시 5% 캐시백을 제공합니다.',
        '대상 도서 가맹점에서 나라사랑카드로 결제합니다.',
        'Life 서비스 전월 이용실적에 따라 월 통합 캐시백 한도 5천원~5만원이 적용됩니다.',
        'https://www.card-gorilla.com/contents/detail/4126', TRUE, 10
    ),
    (
        'SELF_DEVELOPMENT', 'IBK 나라사랑카드 - 어학시험·교보문고 할인', NULL, NULL, 'IBK나라사랑카드 체크 보유자',
        '어학시험 및 교보문고 이용 시 2천원 할인을 제공합니다.',
        '대상 시험 또는 가맹점에서 IBK나라사랑카드로 결제합니다.',
        '병 급여이체 조건 충족 시 전월 이용실적 조건이 면제될 수 있습니다. 세부 적용 조건은 카드사 안내를 확인해야 합니다.',
        'https://www.card-gorilla.com/contents/detail/4126', TRUE, 20
    ),
    (
        'SELF_DEVELOPMENT', '하나 나라사랑카드 - 어학시험·서점 할인', NULL, NULL, '하나 나라사랑카드 체크 보유자',
        '어학시험과 서점 결제 시 5% 할인을 제공합니다.',
        '대상 시험 또는 서점에서 하나 나라사랑카드로 결제합니다.',
        'Basic 서비스 전월 이용실적에 따라 월 통합 할인 한도 5천원~5만원이 적용됩니다.',
        'https://www.card-gorilla.com/contents/detail/4126', TRUE, 30
    ),
    (
        'LODGING', 'IBK 나라사랑카드 - 야놀자 할인', NULL, NULL, 'IBK나라사랑카드 체크 보유자',
        '야놀자 결제 시 10% 할인을 제공합니다.',
        '야놀자에서 IBK나라사랑카드로 결제합니다.',
        '파워업 All in One 서비스의 전월 실적·통합 할인 한도와 제외 조건을 카드사 안내에서 확인해야 합니다.',
        'https://www.card-gorilla.com/contents/detail/4126', TRUE, 10
    ),
    (
        'LODGING', '하나 나라사랑카드 - 국군콘도 할인', NULL, NULL, '하나 나라사랑카드 체크 보유자',
        '국군콘도 이용 시 20% 할인을 제공합니다.',
        '대상 국군콘도를 하나 나라사랑카드로 결제합니다.',
        'Basic 서비스 전월 이용실적에 따라 월 통합 할인 한도 5천원~5만원이 적용됩니다.',
        'https://www.card-gorilla.com/contents/detail/4126', TRUE, 20
    ),
    (
        'ETC', '신한 나라사랑카드 - PX·해군마트 캐시백', NULL, NULL, '신한 나라사랑카드 체크 보유자',
        'PX 및 해군마트 결제 시 20% 캐시백을 제공합니다.',
        'PX 또는 해군마트에서 나라사랑카드로 결제합니다.',
        '월 최대 10만원 캐시백 한도가 적용됩니다. 건당 결제금액에 따른 일·월 횟수 제한은 카드사 안내를 확인해야 합니다.',
        'https://www.card-gorilla.com/contents/detail/4126', TRUE, 10
    ),
    (
        'ETC', 'IBK 나라사랑카드 - PX 최대 할인', NULL, NULL, 'IBK나라사랑카드 체크 보유자',
        'PX 및 해군마트에서 기본할인과 특별할인을 합산해 최대 50% 할인을 제공합니다.',
        'PX 또는 해군마트에서 IBK나라사랑카드로 결제합니다.',
        '전월 실적 없이 적용됩니다. 현역병·병 급여이체자 및 결제금액별 할인율·횟수·월 한도가 다르므로 카드사 안내를 확인해야 합니다.',
        'https://www.card-gorilla.com/contents/detail/4126', TRUE, 20
    ),
    (
        'ETC', '하나 나라사랑카드 - PX·편의점·온라인쇼핑 할인', NULL, NULL, '하나 나라사랑카드 체크 보유자',
        'PX 최대 30%, 쿠팡·네이버플러스 스토어 20%, 편의점 행사품목 10% 할인 혜택을 제공합니다.',
        '대상 가맹점에서 하나 나라사랑카드로 결제합니다.',
        'Easy 서비스는 전월 실적 조건이 없습니다. PX 결제금액별 할인율·월 한도와 편의점 행사 품목 조건은 카드사 안내를 확인해야 합니다.',
        'https://www.card-gorilla.com/contents/detail/4126', TRUE, 30
    ),
    (
        'ETC', 'IBK 나라사랑카드 - 네이버페이 포인트 적립', NULL, NULL, 'IBK나라사랑카드 체크 보유자',
        '네이버페이 결제 시 10% 네이버페이포인트 적립 혜택을 제공합니다.',
        'IBK나라사랑카드를 네이버페이에 등록해 국내 가맹점에서 결제합니다.',
        '월 5회, 최대 5천원 적립 한도가 적용됩니다.',
        'https://www.card-gorilla.com/contents/detail/4126', TRUE, 40
    ),
    (
        'ETC', 'IBK 나라사랑카드 - 해외 결제·ATM 수수료 면제', NULL, NULL, 'IBK나라사랑카드 체크 보유자',
        '해외 결제 수수료와 해외 ATM 인출수수료 면제 혜택을 제공합니다.',
        '해외 결제 또는 해외 ATM 이용 시 IBK나라사랑카드를 사용합니다.',
        '전월 실적 조건이 없으며, 해외 ATM 인출수수료 면제는 월 3회까지 적용됩니다.',
        'https://www.card-gorilla.com/contents/detail/4126', TRUE, 50
    )
ON DUPLICATE KEY UPDATE
    target_text = VALUES(target_text),
    content = VALUES(content),
    usage_method = VALUES(usage_method),
    remark_text = VALUES(remark_text),
    source_url = VALUES(source_url),
    active_yn = VALUES(active_yn),
    display_order = VALUES(display_order),
    updated_at = CURRENT_TIMESTAMP;

COMMIT;
