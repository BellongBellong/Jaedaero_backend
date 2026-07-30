# API 명세 반영표

백엔드 API 명세와 프론트엔드 기능·목 서버 경로를 일치시키기 위한 기준표입니다.

| 도메인                   | 기능                      | Method | Path                                                          | Request                          | Response                            |
| ------------------------ | ------------------------- | ------ | ------------------------------------------------------------- | -------------------------------- | ----------------------------------- |
| 인증·온보딩              | 소셜 로그인               | POST   | /api/v1/auth/login                                            | LoginRequest                     | LoginResponse                       |
| 인증·온보딩              | 토큰 재발급               | POST   | /api/v1/auth/refresh                                          | RefreshTokenRequest              | RefreshTokenResponse                |
| 인증·온보딩              | 로그아웃                  | POST   | /api/v1/auth/logout                                           | -                                | 204 No Content                      |
| 인증·온보딩              | 회원 탈퇴                 | DELETE | /api/v1/users/me                                              | -                                | 204 No Content                      |
| 인증·온보딩              | 닉네임 중복확인           | GET    | /api/v1/users/nickname/availability?nickname=                 | nickname(query)                  | NicknameAvailabilityResponse        |
| 인증·온보딩              | 닉네임 설정·변경          | PUT    | /api/v1/users/nickname                                        | NicknameRequest                  | 204 No Content                      |
| 인증·온보딩              | 프로필 아이콘·배경색 설정 | PUT    | /api/v1/users/profile-appearance                              | ProfileAppearanceRequest         | 204 No Content                      |
| 인증·온보딩              | 약관 동의 기록            | POST   | /api/v1/agreements                                            | UserAgreementRequest             | UserAgreementResponse               |
| 인증·온보딩              | 군인 정보 등록            | POST   | /api/v1/onboarding/military-info                              | MilitaryInfoRequest              | SoldierProfileResponse              |
| 인증·온보딩              | 투자성향 프리뷰           | POST   | /api/v1/onboarding/investment-preference                      | InvestmentPreferenceRequest      | InvestmentPreferencePreviewResponse |
| 인증·온보딩              | 목표 설정                 | POST   | /api/v1/goals                                                 | GoalRequest                      | GoalResponse                        |
| 인증·온보딩              | 목표 조회                 | GET    | /api/v1/goals                                                 | -                                | GoalResponse                        |
| CODEF 계좌 연동          | 계좌 연동 시작            | POST   | /api/v1/accounts/connect                                      | AccountConnectRequest            | CodefConnectionResponse             |
| CODEF 계좌 연동          | 연동계좌 목록 조회        | GET    | /api/v1/accounts                                              | -                                | List<ConnectedAccountResponse>      |
| 홈 대시보드 · 캐시플로우 | 홈 요약 조회              | GET    | /api/v1/dashboard                                             | -                                | DashboardResponse                   |
| 홈 대시보드 · 캐시플로우 | 월별 자산 흐름 조회       | GET    | /api/v1/cashflow?months=                                      | months(query)                    | CashflowForecastResponse            |
| What-if · AI 분석        | 시뮬레이션 실행           | POST   | /api/v1/simulations                                           | SimulationRequest                | SimulationResponse                  |
| What-if · AI 분석        | 시뮬레이션 히스토리       | GET    | /api/v1/simulations?page=&size=                               | page,size(query)                 | List<SimulationResponse>            |
| What-if · AI 분석        | 시뮬레이션 상세           | GET    | /api/v1/simulations/{simulationId}                            | simulationId(path)               | SimulationResponse                  |
| What-if · AI 분석        | AI 분석 요청              | POST   | /api/v1/ai-analyses                                           | AiAnalysisRequest                | AiAnalysisResponse                  |
| What-if · AI 분석        | AI 분석 상세              | GET    | /api/v1/ai-analyses/{analysisId}                              | analysisId(path)                 | AiAnalysisResponse                  |
| What-if · AI 분석        | 추천 전략 적용            | POST   | /api/v1/ai-analyses/{analysisId}/apply                        | analysisId(path)                 | StrategyApplicationResponse         |
| What-if · AI 분석        | 전략 적용 이력            | GET    | /api/v1/strategy-applications?page=&size=                     | page,size(query)                 | List<StrategyApplicationResponse>   |
| 거래내역 · 소비          | 거래내역 조회             | GET    | /api/v1/transactions?accountId=&startDate=&endDate=&category= | TransactionSearchRequest         | List<TransactionResponse>           |
| 거래내역 · 소비          | 거래 카테고리 수정        | PUT    | /api/v1/transactions/{transactionId}/category                 | TransactionCategoryUpdateRequest | TransactionResponse                 |
| 장병내일준비적금         | 적금 정보 조회            | GET    | /api/v1/soldier-savings                                       | -                                | SoldierSavingResponse               |
| 챌린지                   | 동기 그룹·랭킹 조회       | GET    | /api/v1/challenges/group                                      | -                                | ChallengeGroupResponse              |
| 챌린지                   | 투자 뱃지 조회            | GET    | /api/v1/users/investment-badges?page=&size=                   | page,size(query)                 | List<InvestmentBadgeResponse>       |
| 챌린지                   | 오늘의 미션 목록          | GET    | /api/v1/missions/today                                        | -                                | List<MissionResponse>               |
| 챌린지                   | 미션 완료                 | POST   | /api/v1/missions/{missionId}/complete                         | missionId(path)                  | MissionCompletionResponse           |
| 리포트 · 추천 · 혜택     | 전역 리포트               | GET    | /api/v1/reports/discharge                                     | -                                | DischargeReportResponse             |
| 리포트 · 추천 · 혜택     | 금융상품 추천             | GET    | /api/v1/products/recommendations                              | -                                | List<ProductRecommendationResponse> |
| 리포트 · 추천 · 혜택     | 군인 혜택 목록            | GET    | /api/v1/benefits?category=&rank=                              | BenefitSearchRequest             | List<MilitaryBenefitResponse>       |
| 투자 리밸런싱            | 리밸런싱 추천             | GET    | /api/v1/rebalancing/recommendations                           | -                                | RebalancingRecommendationResponse   |
| 투자 리밸런싱            | 리밸런싱 적용             | POST   | /api/v1/rebalancing/recommendations/{rebalancingId}/apply     | rebalancingId(path)              | StrategyApplicationResponse         |
| 알림 · FCM               | 디바이스 토큰 등록        | POST   | /api/v1/device-tokens                                         | DeviceTokenRequest               | DeviceTokenResponse                 |
| 알림 · FCM               | 알림 이력                 | GET    | /api/v1/notifications?page=&size=                             | page,size(query)                 | List<NotificationResponse>          |
| 알림 · FCM               | 알림 읽음                 | PUT    | /api/v1/notifications/{notificationId}/read                   | notificationId(path)             | 204 No Content                      |
| 휴가모드                 | 휴가모드 시작             | POST   | /api/v1/leave-mode                                            | LeaveModeRequest                 | LeaveModeResponse                   |
| 휴가모드                 | 현재 상태                 | GET    | /api/v1/leave-mode/current                                    | -                                | LeaveModeResponse 또는 204          |
| 오늘의 AI 투자리포트     | 오늘 시장 리포트          | GET    | /api/v1/market-reports/today                                  | -                                | TodayMarketReportResponse           |

## 현재 요구사항 대비 API 공백

아래 기능은 기획 요구사항에는 있으나 제공된 API 목록에는 명시적인 엔드포인트가 없습니다.

| 요구 기능                  | 현재 처리 기준                                          | 백엔드 확인 필요 사항             |
| -------------------------- | ------------------------------------------------------- | --------------------------------- |
| 이벤트 등록·수정·삭제      | `DashboardResponse.upcomingEvents`를 조회 전용으로 사용 | 이벤트 CRUD API 추가 여부         |
| 휴가모드 종료              | 현재 상태 조회와 시작 API만 사용                        | 종료 또는 비활성화 API 추가 여부  |
| 알림 수신 설정             | 알림 이력만 구현                                        | 알림 설정 조회·수정 API 추가 여부 |
| 프로필 전체 조회           | 로그인 응답 또는 로컬 온보딩 상태 사용                  | `GET /users/me` 추가 여부         |
| 연동 계좌 해제·수동 동기화 | 목록 조회와 연결 시작만 구현                            | 해제·재동기화 API 추가 여부       |
| 목표 수정                  | `POST /goals`를 생성 또는 upsert로 간주                 | PUT/PATCH 분리 여부               |
| 별도 데일리 금융 브리핑    | `DashboardResponse.dailyBriefing` 사용                  | 별도 브리핑 API 필요 여부         |

명세에 없는 경로를 프론트엔드에서 임의로 호출하지 않습니다.
