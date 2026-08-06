# Implementation Plan: 여행 코스 생성 및 날짜별 경로 표시

**Branch**: `001-trip-route-planning` | **Date**: 2026-08-06 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/001-trip-route-planning/spec.md`

## Summary

서울 관광 장소를 검색하고 날짜별 최대 10곳의 방문 순서를 편집한 뒤 도보·자동차·대중교통 실제 경로를 지도에 표시하는 Android MVP를 만든다. Android 앱은 Kotlin, Jetpack Compose, Material 3와 단방향 상태 흐름을 사용하고, Kotlin/Spring Boot API가 카카오·Google 인증, PostgreSQL 코스 동기화, 카카오 장소·경로 API 중계를 담당한다. 단일 저장소의 `android/`와 `backend/`로 나누어 2명씩 병렬 개발하고 Heroku Student 혜택으로 배포한다.

## Technical Context

**Language/Version**: Kotlin 2.2.x, Java 21 LTS, SQL; Android SDK compile/target은 구현 시점 최신 안정판, minSdk 29(Android 10)  
**Primary Dependencies**: Jetpack Compose BOM 2026.06.00, Material 3, AndroidX Navigation/ViewModel/Room/WorkManager/Credential Manager, Kakao SDK for Android 및 Kakao Map SDK; Spring Boot 4.1, Spring Web MVC, Spring Security, OAuth2 resource support, Spring Data JPA, Flyway, Bean Validation, PostgreSQL Driver  
**Storage**: Heroku PostgreSQL을 서버 원본으로 사용하고 Android Room을 최근 코스 캐시 및 재시도 대기열로 사용  
**Testing**: JUnit 5, Kotest/MockK, Spring Boot Test, Testcontainers(PostgreSQL), Android local unit tests, Compose UI tests, MockWebServer  
**Target Platform**: Android 10(API 29)+; Heroku의 Java 21 Linux 런타임과 관리형 PostgreSQL  
**Project Type**: 단일 저장소의 Android 모바일 앱 + REST 웹 서비스  
**Performance Goals**: 검색 결과 95%를 2초 이내 표시, 일정·경로 갱신 95%를 3초 이내 표시, 다른 기기 동기화를 로그인 후 10초 이내 완료  
**Constraints**: 서울만 지원; 날짜별 장소 최대 10곳; 도보·자동차·대중교통; 팀 내부 사용자 10명 이하; 4주; 학생 혜택 및 무료 API 쿼터 안에서 운영; 혼잡도·공동 편집·실시간 내비게이션 제외  
**Scale/Scope**: Android 2명과 Backend 2명, 핵심 화면 약 6개(로그인, 코스 목록/편집, 장소 검색, 날짜 편집, 지도 경로, 오류/재시도 상태), 단일 활성 MVP 환경

## Constitution Check

*GATE: Phase 0 전 및 Phase 1 설계 후 재검토 결과 PASS.*

- **Branch and PR — PASS**: `001-trip-route-planning` 기능 브랜치에서만 작업하고 `main`/`develop` 반영은 승인된 PR로 수행한다.
- **Human AI review — PASS**: Android·Backend 각 담당자가 자신의 변경을 직접 검토하고 PR에 중요한 AI 보조 사실과 검증 결과를 기록한다.
- **Secret management — PASS**: 카카오 REST/Admin 키, Google OAuth secret, 토큰 서명 키, `DATABASE_URL`은 Heroku Config Vars 또는 로컬 환경 변수로만 주입한다. Android에는 플랫폼 제한이 적용된 공개용 Native App Key와 OAuth client identifier만 둔다.
- **Shared contract approval — PASS WITH REQUIRED TEAM ACTION**: [contracts/openapi.yaml](contracts/openapi.yaml)과 [data-model.md](data-model.md)를 구현 전 네 명이 합의하고 PR 또는 이슈에 승인 기록을 남긴다. 첫 배포이므로 호환 마이그레이션은 없고 Flyway로 초기 스키마를 재현한다.
- **Core logic tests — PASS**: 날짜·장소 중복, 날짜별 10곳 제한, 순서 재배열, 서울 범위, 소유권, 버전 충돌, 공급자 실패 시 보존을 단위·통합 테스트한다.
- **Ownership scope — PASS**: Android 팀은 `android/`, Backend 팀은 `backend/`, 공유 계약은 `specs/001-trip-route-planning/contracts/`를 공동 소유한다. 교차 변경은 두 팀 리뷰를 요구한다.
- **Explainable ownership — PASS**: 각 PR 작성자는 상태 흐름, 인증·동기화, 외부 API 변환, 실패 처리와 테스트를 설명한다.

## Project Structure

### Documentation (this feature)

```text
specs/001-trip-route-planning/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── openapi.yaml
└── tasks.md
```

### Source Code (repository root)

```text
android/
├── app/
│   └── src/
│       ├── main/kotlin/com/gilpick/
│       │   ├── app/
│       │   ├── core/{auth,design,network,database,model}/
│       │   └── feature/{login,trips,places,schedule,map}/
│       ├── test/
│       └── androidTest/
├── build.gradle.kts
└── settings.gradle.kts

backend/
├── src/
│   ├── main/kotlin/com/gilpick/
│   │   ├── auth/
│   │   ├── trip/
│   │   ├── place/
│   │   ├── route/
│   │   └── common/
│   ├── main/resources/
│   │   ├── application.yml
│   │   └── db/migration/
│   └── test/kotlin/com/gilpick/
├── build.gradle.kts
├── settings.gradle.kts
├── Procfile
└── system.properties
```

**Structure Decision**: 하나의 저장소에서 Android와 Backend를 최상위 디렉터리로 분리한다. 기능별 패키지를 양쪽에서 같은 도메인 이름으로 맞추되 구현 모델을 공유 라이브러리로 결합하지 않고 OpenAPI 계약을 경계로 둔다. 이 방식은 2+2 병렬 작업과 계약 리뷰를 지원하면서 빌드·배포를 독립적으로 유지한다.

## Delivery Strategy

1. **Week 1 — 기반과 계약**: 저장소 골격, CI, PostgreSQL/Flyway, OpenAPI 승인, Android 디자인·네트워크 기반, 카카오·Google 로그인 세로 슬라이스.
2. **Week 2 — 장소와 일정**: 서울 장소 검색, 코스·날짜 CRUD, 중복·10곳 제한, Room 캐시, 서버 동기화.
3. **Week 3 — 지도와 경로**: 카카오 지도, 도보·자동차·대중교통 구간 조회, 날짜별 경로 조합, 오류·쿼터 처리.
4. **Week 4 — 통합과 출시 준비**: 다른 기기 복원, 충돌·실패 테스트, Compose UI 테스트, Heroku 배포, 내부 10명 수동 검증 및 결함 수정.

각 주의 끝에는 Android와 Backend가 함께 실행되는 수직 기능을 시연한다. 4주 내 위험이 현실화되면 기능을 삭제하지 않고 내부 배포 일정을 조정하며, 특히 인증·동기화·경로 정확성을 미검증 상태로 출시하지 않는다.

## Complexity Tracking

헌법 위반은 없다. 단일 저장소 안의 두 실행 애플리케이션은 모바일과 서버의 배포·비밀 경계가 달라 필요한 최소 분리이다.
