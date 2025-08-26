# Tosk Backend

Spring Boot 3.1 / Java 21 ベースの Todo + SNS アプリケーションのバックエンドです。

---

## アーキテクチャ概要

* レイヤード構造: API → Application → Domain → Infra → DB
* DB: PostgreSQL + Flyway (UUID ID, 監査列付き)
* セキュリティ: Spring Security + JWT (HttpOnly/Secure Cookie)
* 品質ゲート: Spotless / Checkstyle / PMD / SpotBugs / JaCoCo【295†overview\.md†L70-L83】【298†backend\_coding\_style.md†L243-L251】

詳細は [doc/overview.md](doc/overview.md) を参照。

---

## ローカル開発フロー

### 1. コード整形

```bash
./gradlew spotlessApply
```

Google Java Format に従って自動整形します。差分があれば必ずコミットしてください。

### 2. 静的解析

```bash
./gradlew checkstyleMain checkstyleTest
./gradlew pmdMain pmdTest
./gradlew spotbugsMain spotbugsTest
```

Checkstyle / PMD / SpotBugs による品質ゲート。違反があれば修正してください。

### 3. テスト & カバレッジ

```bash
./gradlew test jacocoTestReport jacocoTestCoverageVerification
```

JUnit テストを実行し、JaCoCo レポートを生成。命令網羅率 80%、分岐網羅率 70% を満たさないと失敗します【299†strategy.md†L16-L24】。

### 4. ビルド（統合）

```bash
./gradlew build
```

すべてのチェックとテストを通過した状態でビルドします。

---

## レポート出力先

* Spotless: `build/reports/spotless/`
* Checkstyle: `build/reports/checkstyle/`
* PMD: `build/reports/pmd/`
* SpotBugs: `build/reports/spotbugs/`
* JaCoCo: `build/reports/jacoco/test/html/index.html`

---

ドキュメントは [doc](doc) を参照。
