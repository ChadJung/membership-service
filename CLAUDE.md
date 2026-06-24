# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Baemin membership subscription service — a Spring Boot application managing membership lifecycle (subscribe, cancel, renew), recurring payments, and grade-based benefits. Korean-language domain; log messages and error messages are in Korean.

Context: portfolio / mock project for a 우아한형제들(Woowa Brothers) growth-product job posting.

## Build & Run Commands

> On Windows/PowerShell (this environment) use `.\gradlew.bat` instead of `./gradlew`.
> There is no lint/format task (no Checkstyle/Spotless) — `build` and `test` are the only quality gates.

```bash
# All commands run from the project root (this directory).

# Build (skip tests)
./gradlew build -x test

# Run tests
./gradlew test

# Run a single test class
./gradlew test --tests "com.baemin.membership.domain.member.service.MemberServiceTest"

# Run a single test method
./gradlew test --tests "com.baemin.membership.domain.member.service.MemberServiceTest.subscribe_success"

# Start the application (local profile uses H2 in-memory DB)
./gradlew bootRun

# Start infrastructure (MySQL, Redis, Kafka, Prometheus, Grafana)
docker-compose up -d
```

## Architecture

- **Java 17, Spring Boot 3.2, Gradle (Kotlin DSL)**
- **Package structure**: `com.baemin.membership` with domain-driven layout
  - `domain/{member,payment,benefit}/` — each has `controller`, `dto`, `entity`, `repository`, `service` layers
  - `global/` — cross-cutting: config, events, exceptions, response wrapper, schedulers

### Key Domain Flows

- **Subscribe**: `MemberController` → `MemberService.subscribe()` → publishes `MembershipEvent` → `KafkaEventPublisher` sends to `membership-events` topic
- **Payment**: `PaymentController` → `PaymentService.processPayment()` → publishes `PaymentEvent` → Kafka `payment-events` topic
- **Scheduled renewals**: `PaymentScheduler` (cron `0 0 6 * * *`, daily 06:00) triggers `PaymentService.processScheduledRenewals()`, which finds `COMPLETED` payments whose `nextPaymentDate <= now` and bills each. Per-item failures are caught and logged so the loop continues — there is no retry or dead-letter handling.

### Domain Model Conventions

- **Rich entities, anemic DTOs**: business logic lives on JPA entities — `Member.cancel/renew/upgradeGrade`, `Payment.complete/fail`. Entities use a `@Builder` + protected no-arg constructor (no public setters); mutate via these domain methods. DTOs are Java `record`s (`request.userId()` accessor style).
- **Billing cycle**: 1 month. Subscribe and `renew()` set `expiredAt` to `now + 1 month`; payment amount comes from `MembershipGrade.getMonthlyFee()` (BASIC 2990, PREMIUM 7900 KRW), never from the client request.
- **Active-membership invariant**: subscribe rejects an existing `ACTIVE` membership; payment/cancel require an `ACTIVE` membership keyed by `userId`.

### Event System

Spring `ApplicationEventPublisher` internally, then `KafkaEventPublisher` (`@Async @EventListener`) bridges to Kafka topics (`membership-events`, `payment-events`).

### Profiles & Data

- `local` (default): H2 in-memory with `ddl-auto: create-drop`, no external infra needed for basic dev
- `prod`: MySQL 8, requires running docker-compose services
- Jackson uses `SNAKE_CASE` naming globally
- JPA `open-in-view: false`, batch fetch size 100

### Error Handling

`BusinessException` + `ErrorCode` enum → `GlobalExceptionHandler` returns `ApiResponse` wrapper. Error codes are prefixed by domain: `MEMBER_`, `PAYMENT_`, `BENEFIT_`, `COMMON_`.

### Monitoring

Actuator exposes `/health`, `/info`, `/prometheus`, `/metrics`. Custom Micrometer counters for subscribe/cancel events. Prometheus + Grafana in docker-compose.

## Testing Conventions

- Unit tests use Mockito (`@ExtendWith(MockitoExtension.class)`) with BDDMockito style (`given`/`willReturn`)
- Assertions use AssertJ (`assertThat`)
- Test names use `@DisplayName` in Korean describing the scenario
- Tests use `SimpleMeterRegistry` as a stand-in for Micrometer
