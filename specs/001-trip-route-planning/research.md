# Research: 여행 코스 생성 및 날짜별 경로 표시

## Android 앱

- **Decision**: Kotlin 2.2.x, Android 10+, Jetpack Compose BOM 2026.06.00, Material 3를 사용한다.
- **Rationale**: 팀이 Kotlin/Android 경험을 보유하고 신규 Android 앱이므로 선언형 UI와 공식 안정 BOM이 4주 개발에 가장 직접적이다. Android 공식 문서의 2026.06 안정 BOM을 사용하면 Compose 구성 요소 버전을 호환되게 정렬할 수 있다.
- **Alternatives considered**: XML View는 팀의 신규 UI 생산성을 낮추고, Flutter/React Native는 Kotlin 경험을 활용하지 못하며, Android 8 지원은 테스트 범위를 넓힌다.
- **Source**: https://developer.android.com/develop/ui/compose/bom

## Android 상태와 캐시

- **Decision**: ViewModel 기반 단방향 상태 흐름, Repository 경계, Room 캐시, WorkManager 재시도를 사용한다. 서버가 원본이며 로컬 변경은 온라인 요청 성공 후 확정하고 실패 시 명시적 재시도 상태로 남긴다.
- **Rationale**: 네트워크 실패에도 마지막 정상 코스를 보여 주고 UI와 전송 로직을 분리해 핵심 규칙을 테스트할 수 있다.
- **Alternatives considered**: 서버만 조회하면 장애 때 코스가 사라져 보이며, 완전한 오프라인 우선 양방향 병합은 4주 MVP에 과도하다.

## 인증

- **Decision**: Android는 Kakao SDK와 Android Credential Manager의 Sign in with Google을 사용한다. Backend는 공급자 토큰의 서명·audience·issuer·만료를 검증하고 내부 사용자와 연결한 뒤 짧은 수명의 access token과 회전 가능한 refresh token을 발급한다.
- **Rationale**: 자체 비밀번호 저장을 피하면서 두 로그인 공급자를 하나의 내부 사용자/권한 모델로 통합한다. Credential Manager는 Google이 권장하는 Android 통합 경로다.
- **Alternatives considered**: 이메일·비밀번호는 보안·복구 범위를 늘리고, Firebase Authentication은 카카오 사용자 연결과 Spring 권한 모델에 추가 어댑터가 필요하다.
- **Source**: https://developers.google.com/identity/android-credential-manager

## 장소·지도·경로

- **Decision**: Kakao Map Android SDK로 지도를 표시하고, Backend가 Kakao Map REST의 키워드 장소 검색·도보·대중교통 경로와 Kakao Navi REST의 자동차 경로를 호출한다. 여러 장소 경로는 인접 장소 쌍을 순서대로 조회해 하나의 날짜별 응답으로 조합한다.
- **Rationale**: 서울 데이터 품질과 한국어 장소 정보를 우선하며 하나의 개발자 생태계로 공급자 수를 줄인다. 공식 문서는 장소명·주소·카테고리·좌표 검색과 대중교통·도보 경로를 제공하며, 무료 쿼터는 내부 10명 검증에 충분하다.
- **Alternatives considered**: Google Maps Platform은 결제 활성화가 필요하고 서울 국내 데이터 우선 목표와 무료 운영 제약에 덜 맞는다. 여러 국내 공급자 혼합은 좌표·장소 ID 불일치와 장애 처리를 늘린다.
- **Sources**: https://developers.kakao.com/docs/en/kakaomap/common, https://developers.kakao.com/docs/en/local/dev-guide, https://developers.kakao.com/docs/en/getting-started/quota

## Backend

- **Decision**: Kotlin 2.2.x, Java 21, Spring Boot 4.1, Spring MVC/Security/Data JPA, Flyway를 사용한다.
- **Rationale**: Android와 같은 언어로 팀 전체 이해도를 높이고, Spring의 인증·검증·트랜잭션·운영 기능을 활용한다. Spring Boot 4.1은 공식 현재 안정 릴리스이며 Kotlin 2.2.x 이상을 지원한다.
- **Alternatives considered**: Ktor는 가볍지만 인증·데이터·운영 구성을 더 직접 조립해야 하고, JavaScript/Python은 팀 경험과 맞지 않는다.
- **Sources**: https://spring.io/projects/spring-boot/, https://docs.spring.io/spring-boot/reference/features/kotlin.html

## 데이터베이스와 동기화

- **Decision**: PostgreSQL을 서버 원본으로 사용하고 UUID 식별자, UTC timestamp, aggregate `version`을 둔다. 코스 변경 요청은 현재 version을 보내며 서버 version과 다르면 `409 Conflict`와 최신 표현을 반환한다.
- **Rationale**: 사용자·코스·날짜·순서 관계와 유일성 제약을 트랜잭션으로 보장하고, 조용한 덮어쓰기 대신 클라이언트가 충돌을 알 수 있다.
- **Alternatives considered**: 문서 데이터베이스는 순서·중복·소유권 제약을 애플리케이션에 더 많이 떠넘기고, 무조건 마지막 쓰기 승리는 다른 기기 변경 손실을 숨긴다.

## 배포와 비용

- **Decision**: GitHub Student Developer Pack의 Heroku 월 크레딧으로 Backend와 Heroku Postgres를 운영한다. Gradle Wrapper, Java 21, `Procfile`, Config Vars, Flyway release 절차를 사용한다.
- **Rationale**: 서버 관리 부담이 적고 Spring Boot 자동 감지 및 Java 빌드팩을 제공한다. 현재 학생 혜택은 월 13달러를 24개월 제공하여 내부 MVP에 맞는다.
- **Alternatives considered**: DigitalOcean 학생 크레딧은 더 자유롭지만 OS·네트워크·DB 운영 부담이 크고, Appwrite는 선택한 Spring Backend와 역할이 겹친다.
- **Sources**: https://education.github.com/pack, https://devcenter.heroku.com/articles/java-support

## 테스트와 관측

- **Decision**: Backend 핵심 규칙은 JUnit 5/Kotest와 Testcontainers PostgreSQL로, Android 상태/변환은 local test와 MockWebServer로, 주요 여정은 Compose UI test로 검증한다. `/actuator/health`와 구조화 로그에 correlation ID·공급자 상태·지연을 남기되 토큰, 검색 원문 개인정보, API 키는 기록하지 않는다.
- **Rationale**: 팀 헌법의 정상·경계·실패 테스트를 충족하고 무료 운영에서 장애 원인을 찾을 최소 신호를 확보한다.
- **Alternatives considered**: 실제 외부 API에만 의존하는 테스트는 느리고 쿼터와 데이터 변화 때문에 반복 가능하지 않다.
