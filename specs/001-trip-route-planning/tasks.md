# Tasks: 여행 코스 생성 및 날짜별 경로 표시

**Input**: Design documents from `/specs/001-trip-route-planning/`  
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/openapi.yaml, quickstart.md

**Tests**: 팀 헌법에 따라 핵심 비즈니스 규칙의 정상·경계·실패 테스트는 필수이며 구현 전에 작성한다.

**Organization**: Android 2명과 Backend 2명이 병렬 작업할 수 있도록 공통 기반 이후 사용자 스토리별 수직 기능으로 구성한다.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: 선행 작업 완료 후 다른 파일에서 병렬 수행 가능
- **[Story]**: 명세의 User Story 0–3과 연결
- 모든 작업은 수정할 정확한 파일 경로를 포함

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: 단일 저장소 안에 독립 빌드 가능한 Android와 Backend 프로젝트 및 검증 기반을 만든다.

- [X] T001 Initialize the Android Kotlin project with minSdk 29 and Compose BOM 2026.06.00 in `android/settings.gradle.kts`, `android/build.gradle.kts`, and `android/app/build.gradle.kts`
- [X] T002 [P] Initialize the Kotlin 2.2.x, Java 21, Spring Boot 4.1 Gradle project in `backend/settings.gradle.kts` and `backend/build.gradle.kts`
- [X] T003 [P] Configure Android formatting, static analysis, and unit/UI test jobs in `.github/workflows/android-ci.yml`
- [X] T004 [P] Configure Backend formatting, unit/integration tests, and OpenAPI validation jobs in `.github/workflows/backend-ci.yml`
- [X] T005 [P] Add repository ignores and shared formatting rules without secret values in `.gitignore` and `.editorconfig`
- [X] T006 [P] Add placeholder-only Backend environment documentation in `backend/.env.example`
- [X] T007 [P] Configure Heroku Java 21 startup and migration commands in `backend/Procfile` and `backend/system.properties`
- [ ] T008 Record the four-person approval of the initial OpenAPI contract and database model in `specs/001-trip-route-planning/approvals.md`

**Checkpoint**: 두 프로젝트가 빈 상태로 빌드되고 CI가 실행되며 공유 계약 합의가 기록된다.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: 모든 사용자 스토리가 공유하는 데이터, 보안, 네트워크, 오류 및 테스트 기반을 구축한다.

**⚠️ CRITICAL**: 이 단계가 끝나기 전에는 사용자 스토리 구현을 병합하지 않는다.

- [ ] T009 Configure environment-bound database, OAuth, Kakao, token, and CORS properties with startup validation in `backend/src/main/resources/application.yml` and `backend/src/main/kotlin/com/gilpick/common/config/AppProperties.kt`
- [ ] T010 [P] Configure PostgreSQL, Flyway, and JPA auditing in `backend/src/main/kotlin/com/gilpick/common/config/PersistenceConfig.kt`
- [ ] T011 [P] Create the initial User, AuthIdentity, RefreshSession, Trip, TripDay, PlaceSnapshot, and DayVisit schema with all uniqueness and position constraints in `backend/src/main/resources/db/migration/V1__initial_schema.sql`
- [ ] T012 [P] Implement the common API error envelope and validation/exception mapping in `backend/src/main/kotlin/com/gilpick/common/api/ApiError.kt` and `backend/src/main/kotlin/com/gilpick/common/api/GlobalExceptionHandler.kt`
- [ ] T013 [P] Implement bearer authentication, ownership principal, and public auth endpoint rules in `backend/src/main/kotlin/com/gilpick/common/security/SecurityConfig.kt` and `backend/src/main/kotlin/com/gilpick/common/security/GilpickPrincipal.kt`
- [ ] T014 [P] Add correlation IDs and secret-safe structured request/provider logging in `backend/src/main/kotlin/com/gilpick/common/logging/CorrelationIdFilter.kt`
- [ ] T015 [P] Create PostgreSQL Testcontainers integration-test support and migration verification in `backend/src/test/kotlin/com/gilpick/support/PostgresIntegrationTest.kt` and `backend/src/test/kotlin/com/gilpick/common/MigrationTest.kt`
- [ ] T016 [P] Implement the Android HTTP client, auth interceptor, error mapping, and build-configured base URL in `android/app/src/main/kotlin/com/gilpick/core/network/ApiClient.kt` and `android/app/src/main/kotlin/com/gilpick/core/network/ApiError.kt`
- [ ] T017 [P] Implement encrypted access/refresh token persistence and clearing in `android/app/src/main/kotlin/com/gilpick/core/auth/TokenStore.kt`
- [ ] T018 [P] Create the Room database shell, converters, and migration policy in `android/app/src/main/kotlin/com/gilpick/core/database/GilpickDatabase.kt`
- [ ] T019 [P] Create Material 3 theme, loading/empty/error components, and accessibility semantics helpers in `android/app/src/main/kotlin/com/gilpick/core/design/GilpickTheme.kt` and `android/app/src/main/kotlin/com/gilpick/core/design/StateComponents.kt`
- [ ] T020 [P] Create the Compose navigation host and authenticated/unauthenticated route guards in `android/app/src/main/kotlin/com/gilpick/app/GilpickNavHost.kt`
- [ ] T021 [P] Add Android coroutine, repository, MockWebServer, and Compose test fixtures in `android/app/src/test/kotlin/com/gilpick/support/TestFixtures.kt` and `android/app/src/androidTest/kotlin/com/gilpick/support/ComposeTestRule.kt`

**Checkpoint**: Foundation ready — 인증, 장소, 일정, 지도 작업이 팀별로 시작될 수 있다.

---

## Phase 3: User Story 0 - 로그인 및 코스 동기화 (Priority: P1) 🎯 MVP Foundation

**Goal**: 카카오 또는 Google로 로그인하고 사용자별 코스를 서버에서 조회하며 다른 기기에서 같은 계정의 코스를 복원한다.

**Independent Test**: 공급자별 로그인 후 서버에 미리 준비된 사용자 코스가 노출되고, 초기화된 두 번째 Android 10+ 기기에서 같은 계정으로 로그인하면 10초 이내 동일 코스를 조회하는지 확인한다.

### Tests for User Story 0

- [ ] T022 [P] [US0] Write failing provider-token validation, identity uniqueness, refresh rotation, logout revocation, and ownership tests in `backend/src/test/kotlin/com/gilpick/auth/AuthServiceTest.kt`
- [ ] T023 [P] [US0] Write failing `/auth/exchange`, `/auth/refresh`, and authenticated `/trips` contract tests in `backend/src/test/kotlin/com/gilpick/auth/AuthApiTest.kt`
- [ ] T024 [P] [US0] Write failing Android Kakao/Google success, cancellation, retry, token refresh, and seeded-trip restoration tests in `android/app/src/test/kotlin/com/gilpick/feature/login/LoginViewModelTest.kt` and `android/app/src/androidTest/kotlin/com/gilpick/feature/login/LoginScreenTest.kt`

### Implementation for User Story 0

- [ ] T025 [P] [US0] Implement User, AuthIdentity, and hashed RefreshSession persistence models in `backend/src/main/kotlin/com/gilpick/auth/AuthEntities.kt` and `backend/src/main/kotlin/com/gilpick/auth/AuthRepositories.kt`
- [ ] T026 [P] [US0] Implement Kakao and Google token verifier adapters with issuer, audience, signature, expiry, and nonce checks in `backend/src/main/kotlin/com/gilpick/auth/provider/KakaoTokenVerifier.kt` and `backend/src/main/kotlin/com/gilpick/auth/provider/GoogleTokenVerifier.kt`
- [ ] T027 [US0] Implement identity linking, internal access-token issuance, refresh rotation, and logout revocation in `backend/src/main/kotlin/com/gilpick/auth/AuthService.kt`
- [ ] T028 [US0] Implement auth exchange, refresh, and logout endpoints in `backend/src/main/kotlin/com/gilpick/auth/AuthController.kt`
- [ ] T029 [P] [US0] Implement Kakao SDK and Google Credential Manager sign-in clients in `android/app/src/main/kotlin/com/gilpick/core/auth/KakaoSignInClient.kt` and `android/app/src/main/kotlin/com/gilpick/core/auth/GoogleSignInClient.kt`
- [ ] T030 [US0] Implement login repository, login state machine, and retry/cancellation behavior in `android/app/src/main/kotlin/com/gilpick/feature/login/LoginRepository.kt` and `android/app/src/main/kotlin/com/gilpick/feature/login/LoginViewModel.kt`
- [ ] T031 [US0] Build the Material 3 login screen with separate Kakao/Google actions and accessible failure guidance in `android/app/src/main/kotlin/com/gilpick/feature/login/LoginScreen.kt`
- [ ] T032 [US0] Implement authenticated trip-list retrieval and login-time Room refresh for seeded and previously saved trips in `android/app/src/main/kotlin/com/gilpick/feature/trips/TripSyncRepository.kt`

**Checkpoint**: 인증과 사용자 격리가 독립 검증되며 서버에 존재하는 코스를 새 기기에서 복원할 수 있다.

---

## Phase 4: User Story 1 - 장소 검색 및 코스 추가 (Priority: P1) 🎯 MVP

**Goal**: 서울 지역과 키워드로 장소를 검색하고 결과 정보를 확인하여 선택 날짜에 중복 없이 최대 10곳까지 추가한다.

**Independent Test**: 서울·키워드 검색 결과의 장소명·주소·카테고리·좌표를 확인하고 준비된 날짜에 장소를 추가한 뒤 중복, 11번째, 서울 밖, 결과 없음, 공급자 실패를 검증한다.

### Tests for User Story 1

- [ ] T033 [P] [US1] Write failing Kakao place mapping, Seoul boundary, empty result, quota, timeout, and provider failure tests in `backend/src/test/kotlin/com/gilpick/place/KakaoPlaceClientTest.kt`
- [ ] T034 [P] [US1] Write failing duplicate-place, tenth/eleventh-place, ownership, and canonical position tests in `backend/src/test/kotlin/com/gilpick/trip/AddVisitServiceTest.kt`
- [ ] T035 [P] [US1] Write failing `/places` and trip-update contract tests for search and place addition in `backend/src/test/kotlin/com/gilpick/place/PlaceApiTest.kt`
- [ ] T036 [P] [US1] Write failing Android search debounce, result/empty/error states, add success, duplicate, and limit UI tests in `android/app/src/test/kotlin/com/gilpick/feature/places/PlaceSearchViewModelTest.kt` and `android/app/src/androidTest/kotlin/com/gilpick/feature/places/PlaceSearchScreenTest.kt`

### Implementation for User Story 1

- [ ] T037 [P] [US1] Implement Kakao keyword-search client, provider DTO mapping, timeout, quota, and retry-safe error translation in `backend/src/main/kotlin/com/gilpick/place/KakaoPlaceClient.kt`
- [ ] T038 [US1] Implement Seoul-filtered place search service and `/places` endpoint with paging in `backend/src/main/kotlin/com/gilpick/place/PlaceSearchService.kt` and `backend/src/main/kotlin/com/gilpick/place/PlaceController.kt`
- [ ] T039 [P] [US1] Implement PlaceSnapshot and DayVisit persistence plus duplicate/10-place validation in `backend/src/main/kotlin/com/gilpick/trip/VisitEntities.kt` and `backend/src/main/kotlin/com/gilpick/trip/VisitPolicy.kt`
- [ ] T040 [US1] Implement transactional place addition through the versioned trip update service in `backend/src/main/kotlin/com/gilpick/trip/TripUpdateService.kt`
- [ ] T041 [P] [US1] Implement Android place DTOs, search repository, and Seoul-only query mapping in `android/app/src/main/kotlin/com/gilpick/feature/places/PlaceSearchRepository.kt`
- [ ] T042 [US1] Implement debounced search state, result selection, date selection, add command, and recoverable errors in `android/app/src/main/kotlin/com/gilpick/feature/places/PlaceSearchViewModel.kt`
- [ ] T043 [US1] Build the search/result/add Compose UI including missing-field, no-result, duplicate, limit, and provider-failure messages in `android/app/src/main/kotlin/com/gilpick/feature/places/PlaceSearchScreen.kt`

**Checkpoint**: 준비된 여행 날짜에 서울 장소를 안전하게 추가하는 흐름이 독립 실행된다.

---

## Phase 5: User Story 2 - 날짜별 일정 편집 (Priority: P2)

**Goal**: 여행 코스와 날짜를 만들고 삭제하며 장소 삭제·순서 변경을 서버에 저장하고 기기 간 동기화한다.

**Independent Test**: 두 날짜에 각각 여러 장소를 구성하고 날짜/장소 삭제와 순서 변경을 수행한 후, 다른 초기화 기기에서 같은 계정으로 로그인해 동일한 최신 일정이 10초 이내 복원되는지 확인한다.

### Tests for User Story 2

- [ ] T044 [P] [US2] Write failing trip/date uniqueness, cascade delete, reorder boundaries, ownership, optimistic conflict, and last-good-state tests in `backend/src/test/kotlin/com/gilpick/trip/TripServiceTest.kt`
- [ ] T045 [P] [US2] Write failing `/trips` create/list/get/update/delete contract and `409` latest-representation tests in `backend/src/test/kotlin/com/gilpick/trip/TripApiTest.kt`
- [ ] T046 [P] [US2] Write failing Room mapping, pending sync, conflict refresh, delete confirmation, and reorder state tests in `android/app/src/test/kotlin/com/gilpick/feature/schedule/ScheduleViewModelTest.kt`
- [ ] T047 [P] [US2] Write failing Compose tests for adding/deleting dates, deleting places, reordering, confirmation, and sync errors in `android/app/src/androidTest/kotlin/com/gilpick/feature/schedule/ScheduleScreenTest.kt`

### Implementation for User Story 2

- [ ] T048 [P] [US2] Implement Trip and TripDay JPA models, repositories, ownership queries, and version field in `backend/src/main/kotlin/com/gilpick/trip/TripEntities.kt` and `backend/src/main/kotlin/com/gilpick/trip/TripRepositories.kt`
- [ ] T049 [US2] Implement transactional create/list/get/update/delete, date uniqueness, cascade deletion, canonical reorder, and optimistic conflict behavior in `backend/src/main/kotlin/com/gilpick/trip/TripService.kt`
- [ ] T050 [US2] Implement `/trips` REST endpoints and ownership-safe not-found behavior in `backend/src/main/kotlin/com/gilpick/trip/TripController.kt`
- [ ] T051 [P] [US2] Implement Android Room entities/DAOs for trips, days, visits, versions, and pending operations in `android/app/src/main/kotlin/com/gilpick/core/database/TripCache.kt` and `android/app/src/main/kotlin/com/gilpick/core/database/TripDao.kt`
- [ ] T052 [US2] Implement server-source-of-truth sync, WorkManager retry, canonical refresh, and `409` user guidance in `android/app/src/main/kotlin/com/gilpick/feature/trips/TripSyncRepository.kt` and `android/app/src/main/kotlin/com/gilpick/feature/trips/TripSyncWorker.kt`
- [ ] T053 [US2] Implement date/place edit and reorder state with destructive-date confirmation in `android/app/src/main/kotlin/com/gilpick/feature/schedule/ScheduleViewModel.kt`
- [ ] T054 [US2] Build trip list and date schedule Compose screens with drag/reorder controls, accessible order alternatives, confirmations, and sync state in `android/app/src/main/kotlin/com/gilpick/feature/trips/TripListScreen.kt` and `android/app/src/main/kotlin/com/gilpick/feature/schedule/ScheduleScreen.kt`

**Checkpoint**: 날짜별 일정 편집과 계정 기반 다중 기기 복원이 완전하게 검증된다.

---

## Phase 6: User Story 3 - 날짜별 지도 경로 확인 (Priority: P3)

**Goal**: 선택 날짜와 도보·자동차·대중교통에 따라 순서 마커와 실제 이동 경로를 지도에 표시한다.

**Independent Test**: 0곳, 1곳, 2–10곳인 날짜를 각 교통수단으로 조회하여 빈 상태·단일 마커·순서 마커와 0–9개 경로 구간이 일정과 일치하고 공급자 실패에도 일정이 유지되는지 확인한다.

### Tests for User Story 3

- [ ] T055 [P] [US3] Write failing walk, drive, transit provider mapping, timeout, quota, partial-segment, and coordinate-error tests in `backend/src/test/kotlin/com/gilpick/route/KakaoRouteClientsTest.kt`
- [ ] T056 [P] [US3] Write failing empty, marker-only, 2–10 stop ordering, ownership, and provider-failure route service tests in `backend/src/test/kotlin/com/gilpick/route/RouteServiceTest.kt`
- [ ] T057 [P] [US3] Write failing route endpoint contract tests for all modes and states in `backend/src/test/kotlin/com/gilpick/route/RouteApiTest.kt`
- [ ] T058 [P] [US3] Write failing Android mode selection, stale response cancellation, marker/segment mapping, empty, and failure state tests in `android/app/src/test/kotlin/com/gilpick/feature/map/RouteMapViewModelTest.kt`
- [ ] T059 [P] [US3] Write failing Compose/map integration tests for ordered markers, mode controls, loading, empty, and retry UI in `android/app/src/androidTest/kotlin/com/gilpick/feature/map/RouteMapScreenTest.kt`

### Implementation for User Story 3

- [ ] T060 [P] [US3] Implement Kakao Map walking/transit and Kakao Navi driving clients with common route-segment mapping in `backend/src/main/kotlin/com/gilpick/route/KakaoRouteClients.kt`
- [ ] T061 [US3] Implement adjacent-stop route composition, 0/1-place states, ordered segment validation, and failure preservation in `backend/src/main/kotlin/com/gilpick/route/RouteService.kt`
- [ ] T062 [US3] Implement the dated route endpoint and transport-mode validation in `backend/src/main/kotlin/com/gilpick/route/RouteController.kt`
- [ ] T063 [P] [US3] Implement Android route DTO mapping, repository, and request cancellation on date/mode changes in `android/app/src/main/kotlin/com/gilpick/feature/map/RouteRepository.kt`
- [ ] T064 [US3] Implement selected-date/mode state, loading/empty/marker-only/ready/error transitions, and retry in `android/app/src/main/kotlin/com/gilpick/feature/map/RouteMapViewModel.kt`
- [ ] T065 [US3] Build the Kakao Map Compose host with numbered markers, polylines, mode selector, camera fitting, accessibility summary, and retry UI in `android/app/src/main/kotlin/com/gilpick/feature/map/RouteMapScreen.kt`

**Checkpoint**: 모든 날짜 크기와 세 교통수단의 지도 표현이 최신 일정 순서와 일치한다.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: 내부 10명 배포 전에 성능, 보안, 관측, 문서 및 전체 여정을 검증한다.

- [ ] T066 [P] Add health checks, provider latency/quota metrics, and secret-safe structured logs in `backend/src/main/kotlin/com/gilpick/common/observability/ObservabilityConfig.kt`
- [ ] T067 [P] Add provider timeouts, bounded retries, request-size limits, token expiry policy, and security headers in `backend/src/main/kotlin/com/gilpick/common/config/ResilienceSecurityConfig.kt`
- [ ] T068 [P] Add Android network-security configuration and release shrinker rules without embedded server secrets in `android/app/src/main/res/xml/network_security_config.xml` and `android/app/proguard-rules.pro`
- [ ] T069 Add end-to-end Backend tests covering login → search → schedule → three route modes → second-device restore in `backend/src/test/kotlin/com/gilpick/e2e/MvpJourneyTest.kt`
- [ ] T070 Add Android end-to-end smoke tests for login → search → edit → map → restart restore in `android/app/src/androidTest/kotlin/com/gilpick/e2e/MvpJourneyTest.kt`
- [ ] T071 Measure and record search, route, and synchronization success/latency criteria against internal fixtures in `specs/001-trip-route-planning/performance-results.md`
- [ ] T072 Validate every local and Heroku step in the quickstart and record deviations in `specs/001-trip-route-planning/quickstart.md`
- [ ] T073 Document Heroku Student deployment, Config Vars, Flyway release, rollback, and Kakao quota-alert procedures in `backend/DEPLOYMENT.md`
- [ ] T074 Run an Android 10+ accessibility and Korean copy review for all six core screens and record results in `specs/001-trip-route-planning/accessibility-review.md`
- [ ] T075 Record Android/Backend ownership, contract/schema approval, AI-assisted code review, automated results, and internal 10-user smoke-test evidence in `specs/001-trip-route-planning/release-readiness.md`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 Setup**: 즉시 시작하며 T008 계약 합의가 완료되어야 공유 계약 구현을 병합할 수 있다.
- **Phase 2 Foundation**: Phase 1에 의존하며 모든 사용자 스토리를 차단한다.
- **US0 (Phase 3)**: Foundation 이후 시작한다. 이후 스토리의 실제 사용자·소유권 검증을 제공한다.
- **US1 (Phase 4)**: Foundation과 US0 인증에 의존한다. 준비된 TripDay fixture로 독립 검증 가능하다.
- **US2 (Phase 5)**: Foundation과 US0 인증에 의존하며 US1의 장소 추가를 통합한다. Trip/Day 기반은 Phase 2 스키마로 공유한다.
- **US3 (Phase 6)**: US2의 날짜별 순서가 필요하며, fixture로 Backend 경로 서비스 자체는 US2 UI보다 먼저 병렬 개발할 수 있다.
- **Polish (Phase 7)**: 출시 대상 US0–US3 완료 후 진행한다.

### User Story Completion Order

```text
Setup → Foundation → US0 로그인/소유권
                         ├── US1 장소 검색·추가 ──┐
                         └── US2 날짜 편집·동기화 ├── US3 지도 경로 → Polish
                                                  ┘
```

### Within Each User Story

- 명시된 테스트를 먼저 작성하고 예상 이유로 실패하는지 확인한다.
- Backend entity/client → service → controller 순으로 완성한다.
- Android repository/client → ViewModel → Compose 화면 순으로 완성한다.
- Android와 Backend는 승인된 OpenAPI 계약을 기준으로 병렬 개발한다.
- 체크포인트에서 독립 테스트가 통과하기 전 다음 스토리를 완료 처리하지 않는다.

## Parallel Execution Examples

### US0

- Backend 1: T022, T025, T027, T028
- Backend 2: T023, T026
- Android 1: T024 Android unit test, T029, T030
- Android 2: T024 Compose test, T031, T032

### US1

- Backend 1: T033, T037, T038
- Backend 2: T034, T035, T039, T040
- Android 1: T036 unit test, T041, T042
- Android 2: T036 UI test, T043

### US2

- Backend 1: T044, T048, T049
- Backend 2: T045, T050
- Android 1: T046, T051, T052
- Android 2: T047, T053, T054

### US3

- Backend 1: T055, T060
- Backend 2: T056, T057, T061, T062
- Android 1: T058, T063, T064
- Android 2: T059, T065

## Implementation Strategy

### Four-Week MVP

1. **Week 1**: T001–T032 — Setup, Foundation, 로그인 세로 슬라이스.
2. **Week 2**: T033–T054 — 장소 검색·추가와 날짜 편집·동기화.
3. **Week 3**: T055–T065 — 세 교통수단 지도 경로.
4. **Week 4**: T066–T075 — 전체 여정, 보안, 성능, 배포와 내부 검증.

### Suggested Demonstrable MVP

US0과 US1까지 완료하면 로그인한 사용자가 서울 장소를 검색해 준비된 날짜에 추가하는 첫 시연이 가능하다. 제품 명세의 완전한 첫 MVP는 US0–US3과 필수 Polish 검증을 모두 포함한다.

### Risk Control

- 외부 API 실제 호출은 fixture 기반 계약 테스트와 소수 smoke test로 분리해 무료 쿼터를 보호한다.
- 계약 또는 DB 변경은 T008 승인 기록을 갱신하기 전 구현하지 않는다.
- 4주 일정이 부족하면 미검증 기능을 출시하지 않고 내부 배포 날짜를 조정한다.
- 각 작업 또는 논리적 작업 묶음 단위로 커밋하고 담당 범위를 넘는 변경은 공동 리뷰한다.
