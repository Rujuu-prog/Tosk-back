# テスト戦略（Tosk Backend）

> 本文書は Tosk Backendのテスト戦略を定める。ER ドメイン（User/Team/UserTeam/Task/Comment/Like/Notification）を前提とし、テストレベル・対象範囲・命名規約・カバレッジ基準を明文化する。

---

## 1. テストピラミッド

* **Unit Test（多数・高速）**

    * 対象: ドメインサービス、ポリシー、バリデーション、アプリケーションサービスのロジック
    * 外部依存（DB, API）はモック化

* **Slice Test（中程度）**

    * 対象: Spring スライステスト

        * `@WebMvcTest` (Controller)
        * `@DataJpaTest` (Repository)
    * 入力検証・シリアライズ/デシリアライズ・JPAマッピングを確認

* **Integration Test（少数・重い）**

    * 対象: API〜DB 貫通
    * Testcontainers(PostgreSQL) を用いて実DBと接続
    * ハッピーパスと代表的なエラーパスを網羅

* **Contract Test（必要に応じて）**

    * 外部API連携（将来 GitHub/Slack 等を導入した際）
    * WireMock / Spring Cloud Contract で仕様との乖離を検出

---

## 2. カバレッジ基準

* **Unit Test**: 命令網羅率 80%以上、分岐網羅率 70%以上
* **Integration Test**: 主要ユースケース（タスク作成〜コメント通知まで）を少なくとも1シナリオずつ
* **セキュリティテスト**: 可視性 (private/team/public) ごとのアクセス制御を必ず確認

---

## 3. テストデータ・フィクスチャ

* User/Team/Task/Comment/Like/Notification の最小構成を共通フィクスチャ化
* Testcontainers による実DBへ投入可能なデータセットを準備
* ID は UUID 固定値で再現性を確保

---

## 4. 命名規約

* **テストクラス名**: `XxxServiceTest`, `XxxControllerTest`
* **テストメソッド**: `should_...`, または `given_when_then`
* **例**: `should_reject_task_creation_when_user_not_joined`

---

## 5. TDD 運用

1. Red: 受け入れ条件をテストで表現
2. Green: 最小実装で通す
3. Refactor: 重複除去・命名整理・ポリシー切り出し

* 1ユースケース = 1コミット（テスト→実装→リファクタ）

---

## 6. CI/CD での検証

* GitHub Actions で Unit/Slice/Integration を並列実行
* JaCoCo レポートを生成し、基準未満でビルド失敗
* Testcontainers はキャッシュを活用し待機時間を短縮

---

## 7. レビュー観点

* テストが仕様を正しく表現しているか（コメントで補足可）
* Red→Green→Refactor の流れがPRに反映されているか
* エッジケースや異常系が網羅されているか

---

## 8. 今後の拡張

* フロントエンドとのE2Eテスト（Playwright/Cypress）
* パフォーマンステスト（JMeter, k6）
* セキュリティテスト（脆弱性スキャン、自動ペネトレーションテスト）

---

> この文書は `doc/testing/strategy.md` としてリポジトリに保存し、CI の品質ゲートと合わせて継続的に見直す。
