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
- 有効化: `src/main/resources/application.yml` で `health,info` を公開。
- ヘルスURL: `GET http://localhost:8080/actuator/health`（コンテナ内から curl でヘルスチェック済み）。

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
