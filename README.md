# 멤버십 서비스 (Membership Service)

구독형 멤버십의 **가입 · 해지 · 정기결제 · 등급별 혜택**을 관리하는 **이벤트 기반 마이크로서비스** 프로젝트입니다.
API 게이트웨이, 서비스별 독립 DB, Kafka 이벤트 통신, 캐시 무효화 전략, 모니터링, 데모 프런트엔드까지 포함합니다.

> 도메인·로그·에러 메시지는 한국어로 작성되어 있습니다.

---

## 기술 스택

| 구분 | 기술 |
|---|---|
| Language / Framework | Java 17, Spring Boot 3.2, Spring Cloud Gateway |
| Persistence | Spring Data JPA, MySQL 8 (서비스별 DB), H2 (로컬) |
| Cache | Redis — 회원 스냅샷(60s TTL + 이벤트 무효화), 혜택 목록(30m TTL) |
| Messaging | Apache Kafka — `membership-events`, `payment-events` |
| Monitoring | Spring Actuator, Micrometer, Prometheus, Grafana |
| Frontend | 정적 SPA (게이트웨이 서빙) — 서비스 호출/이벤트 시각화 콘솔 |
| Build / Deploy | Gradle 멀티모듈 (Kotlin DSL), Docker Compose |

---

## 아키텍처

```
                        ┌────────────────────┐
   브라우저(SPA) ──────→ │  api-gateway :8000 │
                        └──────────┬─────────┘
          /memberships…      /payments…       /benefits…
        ┌───────▼──────┐  ┌────────▼───────┐  ┌───────▼────────┐
        │ member-service│  │ payment-service│  │ benefit-service│
        │     :8081     │←─│     :8082      │  │     :8083      │──┐
        │   member_db   │REST│  payment_db  │  │   benefit_db   │  │REST(캐시)
        └───┬───────▲───┘  └───────┬────────┘  └───▲────────────┘  │
            │       │              │               │  Redis ←──────┘
            │       └── payment-events ────────────┼──── Kafka
            └────────── membership-events ─────────┘
```

- **member-service** — 회원/멤버십 상태의 단일 소유자. 내부 API(`/internal/members/**`)로 스냅샷 제공, `membership-events` 발행, `payment-events`를 소비해 멤버십 갱신
- **payment-service** — 결제 처리·정기결제 스케줄러. 결제 인증은 member-service **실시간 REST 호출**(캐시 금지 — 돈 관련 판단은 stale 불허). 결제 완료 시 `payment-events` 발행 → member-service가 만료일 연장 (**이벤트 기반 갱신**)
- **benefit-service** — 등급별 혜택 조회. 회원 스냅샷을 **Redis 60초 캐시** + `membership-events` 소비로 **이벤트 기반 캐시 무효화**
- **api-gateway** — 단일 진입점(8000), 경로 기반 라우팅 + 데모 SPA 서빙

### 서비스 간 통신 규칙

| 상황 | 방식 | 이유 |
|---|---|---|
| 결제 인증 (회원 ACTIVE 확인) | 동기 REST, 캐시 없음 | 과금 판단은 최신 상태 필수 |
| 혜택 조회의 회원 확인 | 동기 REST + 60s 캐시 | 읽기 빈도 높음, 이벤트로 즉시 무효화 |
| 결제 완료 → 멤버십 갱신 | Kafka 이벤트 | 서비스 간 쓰기 결합 제거 (최종 일관성) |
| 회원 상태 변경 → 캐시 무효화 | Kafka 이벤트 | polling 없이 캐시 일관성 유지 |

---

## 도메인 규칙

- **등급**: `BASIC`(베이직, 월 2,990원), `PREMIUM`(프리미엄, 월 7,900원)
- **결제 금액**은 클라이언트 입력이 아닌 **회원 등급의 월 요금**에서 결정 (변조 방지)
- **활성 멤버십 불변식**: 이미 `ACTIVE`면 중복 가입 거부, 결제·해지·혜택 조회는 `ACTIVE` 멤버십 필요
- **청구 주기**: 1개월. 정기결제 스케줄러(매일 06:00)가 `next_payment_date` 도래 건을 일괄 갱신
- **주기당 1회 과금**: 현재 주기를 커버하는 완료 결제가 있으면 재결제 거부(`PAYMENT_003`, 409)
- **만료일 연장**: 갱신은 기존 만료일 기준 +1개월 — 미리 결제해도 남은 기간을 잃지 않음
- **멱등성 키**: `POST /payments`에 `idempotency_key`(선택)를 보내면 같은 키 재요청 시 기존 결제를 반환 (네트워크 재시도 안전)

---

## API (게이트웨이 :8000 경유)

Base path: `/api/v1` · 모든 응답은 `ApiResponse` 래퍼 + JSON 필드는 `snake_case`

| Method | Endpoint | 담당 서비스 | 설명 |
|---|---|---|---|
| `POST` | `/memberships` | member | 멤버십 가입 |
| `GET` | `/memberships/{userId}` | member | 멤버십 조회 |
| `DELETE` | `/memberships/{userId}` | member | 멤버십 해지 |
| `POST` | `/payments` | payment | 결제 처리 |
| `GET` | `/payments/{userId}` | payment | 결제 내역 조회 |
| `GET` | `/benefits/{userId}` | benefit | 등급별 사용 가능 혜택 조회 |

내부 API(게이트웨이 미노출): `GET /internal/members/{userId}`, `/{userId}/active`, `/by-id/{memberId}`

### 요청/응답 예시

**가입** — `POST /api/v1/memberships`
```json
// Request
{ "user_id": 1, "grade": "PREMIUM" }

// Response (201 Created)
{
  "success": true,
  "data": {
    "id": 1,
    "user_id": 1,
    "grade": "PREMIUM",
    "grade_display_name": "프리미엄",
    "status": "ACTIVE",
    "subscribed_at": "2026-06-24T10:00:00",
    "expired_at": "2026-07-24T10:00:00"
  }
}
```

### 에러 코드

| 코드 | HTTP | 의미 |
|---|---|---|
| `MEMBER_001` | 409 | 이미 활성화된 멤버십 존재 |
| `MEMBER_002` | 404 | 멤버십 정보 없음 |
| `PAYMENT_001` | 500 | 결제 처리 실패 |
| `PAYMENT_002` | 404 | 결제 정보 없음 |
| `PAYMENT_003` | 409 | 현재 결제 주기에 이미 완료된 결제 존재 |
| `BENEFIT_001` | 400 | 사용 가능한 혜택 없음 |
| `COMMON_001` | 400 | 잘못된 입력값 |
| `COMMON_002` | 500 | 서버 내부 오류 |

---

## 실행 방법

### 전체 스택 (권장 — Docker만 있으면 됨)

```bash
cp .env.example .env        # 필요 시 비밀번호 수정
docker-compose up -d --build
# → 데모 콘솔: http://localhost:8000
```

샘플 데이터가 자동 시드됩니다 (빈 DB에서 시작할 때 1회):

- **회원 1,000명** (userId 1001~2000) — 등급 약 6:4(BASIC:PREMIUM), 상태 ACTIVE/CANCELLED/EXPIRED 혼합, 가입 시점 최대 24개월 전까지 분포
- **결제 이력 약 12,000건** — 회원별 가입 시점부터의 월별 청구 체인(중간중간 FAILED 건 포함). 활성 회원의 최신 결제만 `next_payment_date`를 가져 정기결제 스케줄러 대상이 한 달에 걸쳐 자연스럽게 분산됨
- **혜택 12종** (BASIC 5 + PREMIUM 전용 7)

userId 1001~1010은 기존 데모 패턴 유지(홀수 BASIC/짝수 PREMIUM, 1009·1010 해지).
회원·결제 시드는 `common`의 `SeedDataSpec`이 userId만으로 결정적으로 생성하므로 두 서비스 DB가 항상 일관됩니다.

| 서비스 | 포트 |
|---|---|
| api-gateway (+ 데모 SPA) | 8000 |
| member-service | 8081 |
| payment-service | 8082 |
| benefit-service | 8083 |
| MySQL / Redis / Kafka | 3306 / 6379 / 9092 |
| Prometheus / Grafana | 9090 / 3000 |

### 개별 서비스 로컬 실행 (H2, 외부 인프라 불필요)

> Windows/PowerShell 환경에서는 `./gradlew` 대신 `.\gradlew.bat`을 사용하세요.

```bash
./gradlew :member-service:bootRun     # :payment-service, :benefit-service, :api-gateway 동일
```

### 빌드 / 테스트

```bash
./gradlew build -x test               # 전체 모듈 빌드
./gradlew test                        # 전체 테스트
./gradlew :member-service:test        # 모듈 단위 테스트
```

---

## 모니터링

4개 서비스 모두 `/actuator/prometheus`를 노출하고 Prometheus가 15초 간격으로 스크랩합니다.
Grafana(3000, admin/admin)에 Spring Boot 대시보드가 프로비저닝되어 있으며, `application` 라벨로 서비스별 필터링이 가능합니다.
member-service는 가입·해지 커스텀 카운터(`membership.subscribe`, `membership.cancel`)를 제공합니다.

---

## 모듈 구조

```
membership-service/
├── common/            # ApiResponse, ErrorCode, 이벤트 계약, MemberSnapshot, 공용 enum
├── member-service/    # 회원 도메인 + 내부 API + payment-events 컨슈머
├── payment-service/   # 결제 도메인 + 스케줄러 + member REST 클라이언트
├── benefit-service/   # 혜택 도메인 + 캐시된 member 클라이언트 + membership-events 컨슈머
├── api-gateway/       # Spring Cloud Gateway + 데모 SPA (static/)
├── infra/             # prometheus, grafana provisioning, mysql init
└── load-test/         # k6 부하 테스트 (게이트웨이 :8000 대상)
```
