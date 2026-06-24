# 배민 멤버십 서비스 (Membership Service)

구독형 멤버십의 **가입 · 해지 · 정기결제 · 등급별 혜택**을 관리하는 Spring Boot 기반 백엔드 서비스입니다.
이벤트 기반 아키텍처와 모니터링까지 포함한 포트폴리오용 프로젝트입니다.

> 우아한형제들(배달의민족) 그로스 프로덕트 직무 지원을 위한 모의 프로젝트입니다. 도메인·로그·에러 메시지는 한국어로 작성되어 있습니다.

---

## 기술 스택

| 구분 | 기술 |
|---|---|
| Language / Framework | Java 17, Spring Boot 3.2 |
| Persistence | Spring Data JPA, MySQL 8 (운영), H2 (로컬) |
| Cache | Redis (`@Cacheable` 혜택 조회 캐싱) |
| Messaging | Apache Kafka (도메인 이벤트 발행) |
| Monitoring | Spring Actuator, Micrometer, Prometheus, Grafana |
| Build | Gradle (Kotlin DSL) |
| Test | JUnit 5, Mockito, AssertJ |

---

## 아키텍처

```
                ┌──────────────┐   ApplicationEvent   ┌─────────────────────┐   Kafka
  HTTP 요청  →  │  Controller  │ ──────────────────→  │ KafkaEventPublisher │ ───────→  membership-events
                │   Service    │   (@Async listener)  │  (@EventListener)   │           payment-events
                └──────┬───────┘                      └─────────────────────┘
                       │ JPA
                ┌──────▼───────┐         ┌─────────────────┐
                │   MySQL/H2   │         │ PaymentScheduler│  매일 06:00 정기결제
                └──────────────┘         └─────────────────┘
```

- **도메인 주도 패키지 구조**: `domain/{member,payment,benefit}` 각 도메인이 `controller / service / repository / entity / dto` 계층을 가짐
- **이벤트 기반 결합도 분리**: 도메인 로직은 Spring `ApplicationEventPublisher`로 이벤트만 발행하고, `KafkaEventPublisher`가 `@Async @EventListener`로 Kafka 토픽에 비동기 브리지
- **풍부한 도메인 모델**: 비즈니스 규칙을 엔티티 메서드(`Member.cancel/renew`, `Payment.complete/refund`)에 위치, setter 미사용

### 핵심 플로우

| 플로우 | 경로 |
|---|---|
| 가입 | `MemberController` → `MemberService.subscribe()` → `MembershipEvent` 발행 |
| 결제 | `PaymentController` → `PaymentService.processPayment()` → `PaymentEvent` 발행 |
| 정기결제 | `PaymentScheduler`(cron `0 0 6 * * *`) → `processScheduledRenewals()` — `next_payment_date`가 도래한 건 일괄 갱신, 건별 실패는 격리 처리 |

---

## 도메인 규칙

- **등급**: `BASIC`(베이직, 월 2,990원), `PREMIUM`(프리미엄, 월 7,900원)
- **결제 금액**은 클라이언트 입력이 아닌 **회원 등급의 월 요금**에서 결정 (변조 방지)
- **활성 멤버십 불변식**: 이미 `ACTIVE`면 중복 가입 거부, 결제·해지·혜택 조회는 `ACTIVE` 멤버십 필요
- **청구 주기**: 1개월. 가입/갱신 시 만료일·다음 결제일을 `now + 1month`로 설정

---

## API

Base path: `/api/v1` · 모든 응답은 `ApiResponse` 래퍼 + JSON 필드는 `snake_case`

| Method | Endpoint | 설명 |
|---|---|---|
| `POST` | `/memberships` | 멤버십 가입 |
| `GET` | `/memberships/{userId}` | 멤버십 조회 |
| `DELETE` | `/memberships/{userId}` | 멤버십 해지 |
| `POST` | `/payments` | 결제 처리 |
| `GET` | `/payments/{userId}` | 결제 내역 조회 |
| `GET` | `/benefits/{userId}` | 등급별 사용 가능 혜택 조회 |

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

**에러 응답** — 예: 중복 가입 (409 Conflict)
```json
{
  "success": false,
  "error": { "code": "MEMBER_001", "message": "이미 활성화된 멤버십이 존재합니다." }
}
```

### 에러 코드

| 코드 | HTTP | 의미 |
|---|---|---|
| `MEMBER_001` | 409 | 이미 활성화된 멤버십 존재 |
| `MEMBER_002` | 404 | 멤버십 정보 없음 |
| `PAYMENT_001` | 500 | 결제 처리 실패 |
| `PAYMENT_002` | 404 | 결제 정보 없음 |
| `BENEFIT_001` | 400 | 사용 가능한 혜택 없음 |
| `COMMON_001` | 400 | 잘못된 입력값 |
| `COMMON_002` | 500 | 서버 내부 오류 |

---

## 실행 방법

> Windows/PowerShell 환경에서는 `./gradlew` 대신 `.\gradlew.bat`을 사용하세요.

### 1. 로컬 실행 (H2 in-memory, 외부 인프라 불필요)

```bash
./gradlew bootRun
# → http://localhost:8080, H2 콘솔: http://localhost:8080/h2-console
```

### 2. 운영 프로파일 (MySQL/Redis/Kafka)

```bash
# 인프라 기동
cp .env.example .env   # 필요 시 비밀번호 수정
docker-compose up -d

# 애플리케이션 기동
SPRING_PROFILES_ACTIVE=prod ./gradlew bootRun
```

| 서비스 | 포트 |
|---|---|
| Application | 8080 |
| MySQL | 3306 |
| Redis | 6379 |
| Kafka | 9092 |
| Prometheus | 9090 |
| Grafana | 3000 |

### 3. 빌드 / 테스트

```bash
./gradlew build -x test     # 빌드 (테스트 제외)
./gradlew test              # 전체 테스트
./gradlew test --tests "com.baemin.membership.domain.member.service.MemberServiceTest"  # 단일 클래스
```

---

## 모니터링

Actuator가 `/actuator/{health,info,prometheus,metrics}`를 노출하며, 가입·해지에 대한 커스텀 Micrometer 카운터(`membership.subscribe`, `membership.cancel`, 등급별 태그)를 제공합니다. Prometheus가 `/actuator/prometheus`를 스크랩하고 Grafana로 시각화합니다.

---

## 설정 / 프로파일

| 프로파일 | DB | DDL | 용도 |
|---|---|---|---|
| `local` (기본) | H2 in-memory | `create-drop` | 로컬 개발 |
| `prod` | MySQL 8 | `validate` | 운영 (docker-compose 필요) |

- Jackson `SNAKE_CASE` 전역 적용
- JPA `open-in-view: false`, batch fetch size 100
- 비밀 값은 환경변수로 외부화 (`.env`는 git 추적 제외, `.env.example` 템플릿 제공)
