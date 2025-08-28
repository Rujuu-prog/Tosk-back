## Tosk Backend

Todo × SNS を組み合わせたタスク管理バックエンド。セキュアで拡張しやすいAPIを提供します。

### Features
- Auth: Spring Security + JWT (HttpOnly/Secure Cookie)
- Teams: join/approve/reject, roles (leader/member)
- Tasks: visibility (private/team/public), priority, due date
- Social: comments (threaded), likes, notifications
- Observability: structured logs, metrics, health probes

### Stack
- Java 21 + Spring Boot 3 / PostgreSQL + Flyway
- OpenAPI 3.1 + Swagger UI / OpenAPI Generator
- Quality gates: Spotless, Checkstyle, PMD, SpotBugs, JaCoCo

### Quickstart
Run with Docker:
```
docker compose up -d --build
```
App: http://localhost:8080

### API Docs
- Spec: `src/main/resources/openapi/api_internal.yaml`
- Swagger UI: `/swagger-ui.html`

### Docs
- Architecture: `doc/architecture/overview.md`
- Security/Auth: `doc/spec/auth/auth_spec.md`
- Versioning & Release: `doc/spec/release/api_versioning_and_release.md`
- Development Guide: `DEVELOPMENT.md`

### License & Contributing
- Proprietary. Contributions via PR are welcome. Please follow coding standards and quality gates.
