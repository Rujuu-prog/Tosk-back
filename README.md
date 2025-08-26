# Tosk Backend

Todo + SNS アプリケーションのバックエンド。

---

## アーキテクチャ概要

* レイヤード: API → Application → Domain → Infra → DB
* DB: PostgreSQL + Flyway（UUID、監査列）
* セキュリティ: Spring Security + JWT（HttpOnly/Secure Cookie）
* 品質ゲート: Spotless / Checkstyle / PMD / SpotBugs / JaCoCo

詳細は [doc/overview.md](doc/overview.md) を参照。

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

* 設計概要: [doc/overview.md](doc/overview.md)
* コーディングガイド: [doc/backend\_coding\_style.md](doc/backend_coding_style.md)
