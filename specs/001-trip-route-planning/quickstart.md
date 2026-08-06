# Quickstart: 길픽 MVP 개발 환경

## Prerequisites

- Android Studio 최신 안정판과 Android 10(API 29) SDK
- Java 21
- Docker Desktop 또는 호환 컨테이너 런타임
- GitHub Student Developer Pack 및 Heroku 학생 혜택
- Kakao Developers 앱(카카오 로그인, Kakao Map, Kakao Navi 활성화)
- Google Cloud OAuth Android/Web client

비밀 값은 저장소에 커밋하지 않는다. `.env.example`에는 변수명과 자리표시자만 둔다.

## Repository setup

```text
android/   Android 앱
backend/   Spring Boot API
specs/001-trip-route-planning/contracts/openapi.yaml   팀 합의 API 계약
```

## Backend local run

1. 로컬 PostgreSQL을 실행한다.
2. 다음 환경 변수를 설정한다: `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`, `KAKAO_REST_API_KEY`, `KAKAO_ADMIN_KEY`, `GOOGLE_OAUTH_CLIENT_ID`, `TOKEN_SIGNING_SECRET`.
3. `backend/`에서 Gradle Wrapper로 테스트를 실행한다.
4. Spring Boot 애플리케이션을 실행하고 `/actuator/health`가 성공하는지 확인한다.
5. Flyway가 빈 데이터베이스에 초기 스키마를 생성하는지 확인한다.

## Android local run

1. `local.properties` 또는 사용자 Gradle 속성에 플랫폼 제한된 Kakao Native App Key와 개발 API base URL을 둔다. 이 파일은 커밋하지 않는다.
2. Google OAuth Android client에 debug signing certificate와 package name을 등록한다.
3. Android 10 이상 에뮬레이터 또는 기기에서 앱을 실행한다.
4. 카카오 로그인과 Google 로그인을 각각 검증한다.

## Required verification before PR

- Backend unit/integration/contract tests
- Android local unit tests와 Compose UI smoke tests
- 날짜 중복, 같은 날짜 장소 중복, 10곳/11번째 경계, 순서 변경, 서울 밖 장소 거절
- 카카오·Google 로그인 성공/취소/실패
- 도보·자동차·대중교통 경로 성공/빈 일정/공급자 실패
- 한 기기 저장 후 다른 기기 또는 초기화된 에뮬레이터에서 복원
- 소스·로그·테스트 산출물에 비밀 값이 없는지 점검

## Heroku deployment

1. GitHub Student Pack에서 Heroku 혜택을 활성화한다.
2. Heroku 앱과 PostgreSQL add-on을 만들고 `backend/`를 빌드 대상으로 설정한다.
3. 모든 서버 비밀을 Config Vars에 등록한다.
4. Java 21과 Gradle Wrapper를 고정하고 Flyway migration을 release 단계에서 실행한다.
5. health check, 로그인, 검색, 세 가지 경로, 동기화를 내부 사용자 계정으로 smoke test한다.
6. 무료/학생 크레딧과 Kakao 일일 쿼터 알림을 설정한다.
