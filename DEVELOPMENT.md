## DEVELOPMENT

This document is for contributors working on the Tosk Backend codebase. For a product overview, see README.md.

## Tech Stack
- Java 21 + Spring Boot 3
- PostgreSQL + Flyway
- Security: Spring Security + JWT (HttpOnly/Secure Cookie)
- Quality gates: Spotless, Checkstyle, PMD, SpotBugs, JaCoCo
- OpenAPI: springdoc (UI) + OpenAPI Generator (codegen)

## Common Commands

| Purpose | Command |
| --- | --- |
| Format | `./gradlew spotlessApply` |
| Static analysis | `./gradlew checkstyleMain checkstyleTest pmdMain pmdTest spotbugsMain spotbugsTest` |
| Tests + Coverage | `./gradlew test jacocoTestReport jacocoTestCoverageVerification` |
| All-in-one (format → clean → checks) | `./gradlew spotlessApply clean check` |
| Build | `./gradlew build` |

Notes:
- `check` runs Checkstyle/PMD/SpotBugs/tests/JaCoCo. Run `spotlessApply` first if formatting fails.
- Coverage thresholds: INSTRUCTION 80% / BRANCH 70% (generated code is excluded).

## Local Development
- Hot reload: `SPRING_PROFILES_ACTIVE=local ./gradlew bootRun`
- `.env` autoload: `bootRun` はプロジェクト直下の `.env` を自動読込します（`KEY=VALUE` 形式、`#`はコメント）。
  - 例: `APP_MAIL_PROVIDER=resend`, `RESEND_API_KEY=...`, `APP_MAIL_FROM=...` などを `.env` に書くと起動時に反映されます。
- DB: Use Docker Compose Postgres (`localhost:5432`) or your local Postgres.
- Profiles: `local` (dev), `docker` (compose), `prod` (production)

## Docker (App + DB)
1) Up
```
docker compose up -d --build
```
2) Logs / Down
```
docker compose logs -f app
docker compose down
```
3) Connection (in-compose)
- JDBC: `jdbc:postgresql://db:5432/tosk` (user/pass `tosk`/`tosk`)
- App: `http://localhost:8080`

### Actuator / Health
- Readiness: `GET /actuator/health/readiness`
- Build info exposed via `springBoot.buildInfo()`

### Docker Hot Reload (Gradle bootRun in container)
Source-mounted hot reload environment using the dev compose override:

```
docker compose -f docker-compose.yml -f docker-compose.dev.yml up app-dev
```

- Mounts repo into container (`./:/workspace`) and runs `./gradlew bootRun`.
- JDWP open on `:5005` (IDE attachable).
- Uses `.env` and `SPRING_PROFILES_ACTIVE=docker` by default.
- Gradle cache persisted via named volume `gradle-cache`.

## OpenAPI
- Spec: `src/main/resources/openapi/api_internal.yaml`
- Swagger UI (runtime): `/swagger-ui.html`
- Code generation:
  - Backend (Java interfaces/models): `./gradlew openApiGenerateBackend`
  - Frontend (TypeScript axios; dev-only): `./gradlew openApiGenerateFrontend`
  - Build hooks: backend codegen runs with `build`; frontend client is published by CI (GitHub Packages) on tags.

See also: `doc/spec/release/api_versioning_and_release.md` for versioning and release policy.

## Seed Data (docker profile)
- Seed SQL: `src/main/resources/db/dev/R__seed.sql` (applied only in `docker` profile)
- Example data: admin/alice users, a team with membership, sample tasks/comments/likes

## Project Reports
- Spotless: `build/reports/spotless/`
- Checkstyle: `build/reports/checkstyle/`
- PMD: `build/reports/pmd/`
- SpotBugs: `build/reports/spotbugs/`
- JaCoCo HTML: `build/reports/jacoco/test/html/index.html`

## Tips
- Use `.env` for local overrides (not committed). See `.env.example`.
- In CI, quality gates are enforced; fix violations locally before pushing.
