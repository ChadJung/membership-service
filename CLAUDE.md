# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Membership subscription service — an event-driven microservices project managing membership lifecycle (subscribe, cancel, renew), recurring payments, and grade-based benefits. Korean-language domain; log messages and error messages are in Korean.

Context: portfolio / mock project.

## Build & Run Commands

> On Windows/PowerShell (this environment) use `.\gradlew.bat` instead of `./gradlew`.
> There is no lint/format task (no Checkstyle/Spotless) — `build` and `test` are the only quality gates.

```bash
# All commands run from the project root (this directory).

# Build all modules (skip tests)
./gradlew build -x test

# Run all tests / one module's tests / a single class
./gradlew test
./gradlew :member-service:test
./gradlew :member-service:test --tests "com.domain.membership.member.service.MemberServiceTest"

# Run one service locally (local profile = H2, no infra needed)
./gradlew :member-service:bootRun

# Full stack: 4 service containers + MySQL, Redis, Kafka, Prometheus, Grafana
docker-compose up -d --build
# Demo SPA + gateway: http://localhost:8000
```

## Architecture

**Gradle multi-module MSA** (Java 17, Spring Boot 3.2, Kotlin DSL):

- `common/` — shared contracts only: `ApiResponse`, `ErrorCode`/`BusinessException`/`GlobalExceptionHandler`, `MembershipGrade`/`MembershipStatus` enums, Kafka event records (`MembershipEvent`, `PaymentEvent`), `MemberSnapshot` (internal API DTO). No entities, no Spring Boot app.
- `member-service/` (8081, member_db) — owns member state. Public API `/api/v1/memberships/**`, internal API `/internal/members/{userId}`, `/{userId}/active`, `/by-id/{memberId}`. Publishes `membership-events`; consumes `payment-events` (COMPLETED → `member.renew()`), which is the ONLY path that extends expiredAt.
- `payment-service/` (8082, payment_db) — payments + renewal scheduler (cron `0 0 6 * * *`). Validates members via live REST (`MemberClient`, never cached — billing must not trust stale ACTIVE). Publishes `payment-events`. Fee always from `MembershipGrade.getMonthlyFee()` (BASIC 2990 / PREMIUM 7900), never from the request.
- `benefit-service/` (8083, benefit_db, Redis) — benefits by grade. `MemberClient` caches member snapshots in Redis (`members::{userId}`, 60s TTL) and `MembershipEventConsumer` evicts on subscribe/cancel events. `BenefitReader` caches benefit lists per grade (30m TTL). RedisConfig uses a JavaTimeModule-aware ObjectMapper (cached DTOs carry LocalDateTime) and caches must round-trip through `GenericJackson2JsonRedisSerializer` — return `ArrayList`, never `Stream.toList()` results, and don't cache empty lists.
- `api-gateway/` (8000) — Spring Cloud Gateway routing `/api/v1/{memberships,payments,benefits}/**` to the services; serves the demo SPA from `src/main/resources/static/`.

Cross-service package roots: `com.domain.membership.{common,member,payment,benefit,gateway}`. Services depend only on `:common`, never on each other's modules — runtime coupling is REST + Kafka only.

### Event Flow

Spring `ApplicationEventPublisher` in-process, then per-service `KafkaEventPublisher` (`@Async @EventListener`) bridges to Kafka as JSON (SNAKE_CASE). Consumers deserialize with the Boot ObjectMapper — keep publisher/consumer Jackson config symmetric. `PaymentEvent` must carry `memberId` (member aggregate id) for the renewal consumer.

### Sample Data

Each service seeds when its table is empty: members userId 1001-1010 (odd BASIC / even PREMIUM, 1009-1010 cancelled), payments for memberId 1-8, five benefits. payment-service's seed assumes member ids 1-10 from a fresh member_db.

### Domain Model Conventions

- Rich entities (`Member.cancel/renew`, `Payment.complete/fail`), `@Builder` + protected no-arg constructor, no setters. DTOs are Java `record`s.
- Active-membership invariant: subscribe rejects an existing ACTIVE membership; payment/cancel/benefits require ACTIVE, keyed by `userId`.
- Jackson SNAKE_CASE globally; `BusinessException` + `ErrorCode` (`MEMBER_`/`PAYMENT_`/`BENEFIT_`/`COMMON_` prefixes) → `ApiResponse` wrapper via common's `GlobalExceptionHandler` (picked up by `scanBasePackages = "com.domain.membership"`).

### Profiles

- `local` (default): per-service H2 in-memory, `ddl-auto: create-drop`
- `prod`: MySQL 8 database-per-service (member_db/payment_db/benefit_db in one container), `ddl-auto: update`; docker-compose passes env (DB_HOST, KAFKA_BOOTSTRAP_SERVERS=kafka:29092 internal listener, MEMBER_SERVICE_URL, REDIS_HOST)

## Testing Conventions

- Unit tests: Mockito (`@ExtendWith(MockitoExtension.class)`), BDDMockito (`given`/`willReturn`), AssertJ, Korean `@DisplayName`
- Cross-service dependencies are mocked at the client boundary (`MemberClient`), not the repository
- member-service has one `@SpringBootTest` + MockMvc + `@EmbeddedKafka` integration test (`MemberControllerTest`)
- Full-stack verification is manual: `docker-compose up --build`, then exercise flows through the gateway (subscribe → benefits → payment → renewal-via-Kafka → cancel)
