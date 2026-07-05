# ADR-0003 — Backend stack: Java 21, Spring Boot 3, Maven, JPA, MySQL 8, Flyway

- **Date:** 2026-07-04
- **Status:** Accepted

## Context

The target job market (HK fintech) is heavily Java/Spring. The owner is migrating from Python and benefits from the ecosystem with maximum documentation, examples, and forgiving tooling. A ledger project needs professional-grade schema management and tests that run against the real database engine.

## Decision

- **Java 21 (LTS)**, **Spring Boot 3.x**, **Maven** (more common in enterprises; friendlier errors for newcomers than Gradle).
- **Spring Data JPA** for persistence; schema managed exclusively by **Flyway** migrations from V1 onward (`ddl-auto: validate`).
- **MySQL 8**, run via Docker Compose from day one.
- **Testing: JUnit 5 + Testcontainers (MySQL)** — no H2. Tests run against the same engine as production to avoid dialect lies.
- **springdoc-openapi** for API docs; Postman collection kept in the repo.
- **No Lombok for now**: common in industry, but hidden bytecode magic hinders a learner. DTOs are Java `record`s; entities use explicit accessors. Revisit once Spring feels boring.
- Frontend (planned, confirm via ADR at Phase 4 entry): **React + Vite + TypeScript + Ant Design**. Next.js consciously not chosen — an admin panel behind a login wall has no SSR/SEO need; a Vite SPA is the architecturally honest choice, and React skills transfer to Next.js in a weekend if a JD demands it.

## Consequences

- ✅ Maximum hiring-market signal for HK; huge documentation surface (which also makes well-specified tasks easy for any AI assistant to execute reliably).
- ✅ Flyway-from-day-one and Testcontainers are themselves professionalism signals.
- ⚠️ Spring's "magic" learning curve — mitigated by the concept-first session protocol (`CLAUDE.md` §3).
- ⚠️ Testcontainers requires Docker locally and in CI (both available).

## Revisit when

A specific JD/interview loop demands otherwise, or Gradle/Kotlin becomes a deliberate learning goal (out of scope now).
