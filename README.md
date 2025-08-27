# Tosk Backend

Todo + SNS アプリケーションのバックエンド。

---

## アーキテクチャ概要

* レイヤード: API → Application → Domain → Infra → DB
* DB: PostgreSQL + Flyway（UUID、監査列）
* セキュリティ: Spring Security + JWT（HttpOnly/Secure Cookie）
* 品質ゲート: Spotless / Checkstyle / PMD / SpotBugs / JaCoCo

---

## よく使うコマンド（早見表）

| 目的                          | コマンド                                                                                        |
| --------------------------- | ------------------------------------------------------------------------------------------- |
| 自動整形（Google Java Format）    | `./gradlew spotlessApply`                                                                   |
| 静的解析（個別）                    | `./gradlew checkstyleMain checkstyleTest` / `pmdMain pmdTest` / `spotbugsMain spotbugsTest` |
| テスト＋レポート                    | `./gradlew test jacocoTestReport jacocoTestCoverageVerification`                            |
| **一括（推奨）**: 整形→クリーン→品質ゲート全部 | `./gradlew spotlessApply clean check`                                                       |
| 一括（整形なし）                    | `./gradlew clean check`                                                                     |
| ビルド                         | `./gradlew build`                                                                           |

> `check` には **Checkstyle / PMD / SpotBugs / テスト / JaCoCo** をぶら下げています。未整形がある場合は `spotlessCheck` で失敗するため、必要に応じて事前に `spotlessApply` を実行してください。

---

## 推奨ローカル開発フロー

1. `spotlessApply`（自動整形）
2. 実装・修正
3. `clean check` で一括検証（静的解析・テスト・カバレッジ）
4. レポート確認 → 修正 → コミット

### カバレッジしきい値

* 命令網羅率（INSTRUCTION）: **80%**
* 分岐網羅率（BRANCH）: **70%**

---

## レポート出力先

* Spotless: `build/reports/spotless/`
* Checkstyle: `build/reports/checkstyle/`
* PMD: `build/reports/pmd/`
* SpotBugs: `build/reports/spotbugs/`（例: `main.html` / `test.html`）
* JaCoCo: `build/reports/jacoco/test/html/index.html`

> 初期構築中のみ一時的に失敗を避けたい場合は、`build.gradle` の設定で `pmd { ignoreFailures = true }` と `tasks.withType(com.github.spotbugs.snom.SpotBugsTask).configureEach { ignoreFailures = true }` を有効化し、レポートは出しつつ `check` を通すこともできます（本運用前に戻してください）。

---

## ドキュメント

* 設計概要: [doc/architecture/overview.md](doc/architecture/overview.md)
* コーディングガイド: [doc/conventions/backend_coding_style.md](doc/conventions/backend_coding_style.md)
* DB 設計: [doc/database/README.md](doc/database/README.md)

---

## Docker で実行（アプリ + DB）

前提: Docker と Docker Compose が利用可能であること（Apple Silicon も対応）。

1) 起動

```sh
docker compose up -d --build
```

2) ログ確認 / 停止

```sh
docker compose logs -f app
docker compose down
```

3) 接続情報（コンテナ間）

- DB: `postgres:16-alpine`
- 接続URL: `jdbc:postgresql://db:5432/tosk`
- ユーザー/パス: `tosk` / `tosk`
- アプリはポート `8080` で待受（ホスト側 `http://localhost:8080`）

4) メモ

- `SPRING_DATASOURCE_*` は `docker-compose.yml` の環境変数で注入しています。
- DB 永続化はボリューム `pgdata` に保存されます。
- Flyway は導入済みです。マイグレーションは `src/main/resources/db/migration` に配置してください（例: `V1__init.sql`）。

### Actuator / Healthcheck

- 依存: `spring-boot-starter-actuator` を追加済み。
- 有効化: `src/main/resources/application.yml` で `health,info` を公開。`probes.enabled=true` により `/actuator/health/liveness` `/readiness` も有効。
- ヘルスURL: `GET http://localhost:8080/actuator/health/readiness`（compose のヘルスチェックで使用）。

### 運用

- ログ: prod プロファイルは JSON 出力（logstash 互換）、開発は可読フォーマット。
- リクエストID: `X-Request-Id` を受理/生成してレスポンスに反映。ログの `rid`/`mdc.requestId` に出力。
- メトリクス: `/actuator/prometheus` を公開（prod/docker）。Micrometer + Prometheus registry を同梱。
- 停止: `server.shutdown=graceful` + 各フェーズ 20s。
- 圧縮: `server.compression.enabled=true`。
- Compose 運用: `restart: unless-stopped` とログローテを設定済み。

### .env の利用

- ルートに `.env` を追加（VCS除外）。`docker compose` 実行時に読み込まれ、DB/アプリの接続設定を注入します。
- 例（同梱の `.env`）:
  - `POSTGRES_DB=tosk`
  - `POSTGRES_USER=tosk`
  - `POSTGRES_PASSWORD=tosk`
  - `SPRING_DATASOURCE_URL=jdbc:postgresql://db:5432/tosk`
  - `SPRING_DATASOURCE_USERNAME=tosk`
  - `SPRING_DATASOURCE_PASSWORD=tosk`
  - `SPRING_PROFILES_ACTIVE=docker`
  - サンプルは `.env.example` を参照

### Seed データ（docker プロファイルのみ適用）

- 開発用 seed は `src/main/resources/db/dev/R__seed.sql` に配置。`application-docker.yml` により docker プロファイルでのみ適用されます。
- 追加されるデータ例:
  - ユーザー: `admin@tosk.local`（Administrator）, `alice@tosk.local`（Alice）
  - チーム: `Core Team`（admin が owner）
  - メンバーシップ: admin=leader, alice=member
  - 例タスク/コメント/いいね/通知
  - すべて冪等に挿入されます。
---

## 開発/本番プロファイルの分離

### 開発（ホットリロード）

- 依存: `spring-boot-devtools` を導入済み（developmentOnly）。
- ホストで実行:
  - `SPRING_PROFILES_ACTIVE=local ./gradlew bootRun`
  - DB は `localhost:5432` の Postgres（compose の `db`）に接続。
- Docker で実行（ソースマウント + bootRun）:
  - `docker compose -f docker-compose.yml -f docker-compose.dev.yml up app-dev`
  - 保存→ビルド（IDE自動ビルド）で再起動されます。JDWP は `:5005`。

### 本番（コンテナで jar 実行）

- `docker compose up -d --build`
- プロファイル: `SPRING_PROFILES_ACTIVE=prod` を本番で設定推奨（`application-prod.yml` 適用、Actuator公開を縮小）。
- 例: `SPRING_PROFILES_ACTIVE=prod docker compose up -d --build`
