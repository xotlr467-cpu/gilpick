# Data Model: 여행 코스 생성 및 날짜별 경로 표시

모든 서버 식별자는 UUID, 시각은 UTC instant로 저장한다. 사용자가 입력·조회하는 여행 날짜는 서울 현지 달력 날짜(`YYYY-MM-DD`)다.

## User

| Field | Constraint |
|---|---|
| id | UUID, primary key |
| displayName | 1–100자 |
| createdAt, updatedAt | required |

한 사용자는 여러 로그인 신원과 여러 여행 코스를 가질 수 있다.

## AuthIdentity

| Field | Constraint |
|---|---|
| id | UUID, primary key |
| userId | User FK, required |
| provider | `KAKAO` 또는 `GOOGLE` |
| providerSubject | 공급자가 보장하는 불변 사용자 ID |
| createdAt | required |

`(provider, providerSubject)`는 전체에서 유일하다. 이메일은 계정 식별이나 자동 병합 기준으로 사용하지 않는다.

## RefreshSession

| Field | Constraint |
|---|---|
| id | UUID, primary key |
| userId | User FK, required |
| tokenHash | 원문을 저장하지 않는 해시, unique |
| expiresAt, createdAt | required |
| revokedAt | nullable |

로그아웃 또는 회전 시 폐기한다. 만료·폐기 세션은 access token을 갱신할 수 없다.

## Trip

| Field | Constraint |
|---|---|
| id | UUID, primary key |
| ownerUserId | User FK, required |
| title | 1–100자 |
| version | 양의 정수, 변경마다 1 증가 |
| createdAt, updatedAt | required |

모든 조회·변경은 `ownerUserId`가 인증 사용자와 일치해야 한다. MVP는 공동 소유와 공유를 허용하지 않는다.

## TripDay

| Field | Constraint |
|---|---|
| id | UUID, primary key |
| tripId | Trip FK, required |
| date | 서울 현지 달력 날짜 |
| createdAt, updatedAt | required |

`(tripId, date)`는 유일하다. Trip 삭제 시 함께 삭제된다.

## PlaceSnapshot

코스 등록 당시의 표시 정보를 보존하며 공급자 검색 결과가 바뀌어도 일정이 사라지지 않게 한다.

| Field | Constraint |
|---|---|
| id | UUID, primary key |
| provider | `KAKAO` |
| providerPlaceId | 공급자 장소 ID |
| name | required, 1–200자 |
| address | nullable, 최대 300자 |
| category | nullable, 최대 200자 |
| latitude | -90..90 |
| longitude | -180..180 |
| seoulVerified | true만 일정 등록 가능 |

`(provider, providerPlaceId)`는 유일하다.

## DayVisit

| Field | Constraint |
|---|---|
| id | UUID, primary key |
| tripDayId | TripDay FK, required |
| placeSnapshotId | PlaceSnapshot FK, required |
| position | 1..10 |
| createdAt, updatedAt | required |

`(tripDayId, placeSnapshotId)`와 `(tripDayId, position)`은 각각 유일하다. 날짜별 행 수는 최대 10이며 추가·삭제·재정렬은 한 트랜잭션에서 연속 position으로 정규화한다.

## RouteRequest (non-persistent response model)

| Field | Constraint |
|---|---|
| tripDayId | required |
| mode | `WALK`, `DRIVE`, `TRANSIT` |
| tripVersion | 요청 기준 version |

## RoutePlan (cache optional)

| Field | Constraint |
|---|---|
| mode | required |
| orderedStops | 일정과 동일한 1..10개 장소 |
| segments | 인접 장소 사이 0..9개 구간 |
| geometry | 지도 표시용 좌표열 |
| durationSeconds, distanceMeters | 공급자가 제공할 때만 존재 |
| generatedAt | required |

경로는 일정 또는 교통수단이 바뀌면 다시 조회한다. 공급자 응답 원문은 영구 저장하지 않는다.

## State Transitions

- `Unauthenticated → Authenticated`: 공급자 토큰 검증 성공.
- `Authenticated → Unauthenticated`: refresh session 폐기 또는 만료.
- `Trip vN → Trip vN+1`: 날짜·장소·순서 변경이 현재 version과 일치하고 모든 제약을 통과.
- `Pending local change → Synced`: 서버가 새 version과 canonical representation을 반환.
- `Pending local change → Conflict`: 서버가 409와 최신 Trip을 반환. MVP는 최신 서버 내용을 보여 주고 사용자가 자신의 동작을 다시 수행하도록 안내한다.
- `Route loading → Ready | Empty | Failed`: 2곳 이상이면 경로 조회, 1곳이면 marker-only, 0곳이면 empty 안내, 공급자 오류면 기존 일정 보존.
