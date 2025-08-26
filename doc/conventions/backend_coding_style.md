# 規約

---

## 1. フォルダ構成

```
tosk-back/
  ├─ build.gradle.kts
  ├─ doc/                      # ドキュメント類（要件定義、規約、TDDガイド）
  ├─ src
  │   ├─ main
  │   │   ├─ java/com/tosk
  │   │   │   ├─ common/       # 共通: config, exception, error, security, util
  │   │   │   ├─ user/         # User機能
  │   │   │   ├─ team/         # Team/UserTeam機能
  │   │   │   ├─ task/         # Task機能
  │   │   │   ├─ comment/      # Comment機能
  │   │   │   ├─ like/         # Like機能
  │   │   │   └─ notification/ # Notification機能
  │   │   └─ resources
  │   │       ├─ application.yml
  │   │       └─ db/migration  # Flyway
  │   └─ test/java/com/tosk    # Unit/Slice/Integration テスト
```

* **doc/**: 設計資料・規約を集約し、コードと同じリポジトリで管理。
* **feature-first**: 各ドメイン単位で閉じたパッケージを持ち保守性を高める。
* **layered**: API→Application→Domain→Infra の依存方向を一方通行に維持。

---

## 2. 命名・コードスタイル

* **Java**: 21 を前提。
* **クラス**: `PascalCase` / **メソッド・変数**: `lowerCamel` / **定数**: `UPPER_SNAKE`。
* **DTO**: `XxxRequest`, `XxxResponse`, `XxxSummaryResponse`。Java **record** 推奨。
* **Service**: ユースケース名（例: `CreateTaskService`）。
* **Mapper**: `XxxMapper`（MapStruct 使用）。
* **コメント**: 意図（Why）を残す。公開APIはJavadoc必須。

---

## 3. 例外・エラーハンドリング

* **ドメイン例外**: `DomainException`。業務ルール違反を明示。
* **API例外**: `@ControllerAdvice` で集約し、エラーレスポンスを統一。
* **エラーレスポンス形式**: `code / message / details / debugId` を共通化。

---

## 4. バリデーション

* **Bean Validation**: `@NotBlank`, `@Size`, `@Positive`, `@Valid` 等。
* **DTOで形式検証**、**ドメインでビジネス検証**。
* 制約例:

    * Task: title 1–120文字, description ≤10,000文字, dueDate ≥ 今日, priority ∈ {low,medium,high}, visibility ∈ {private,team,public} ([詳細](../er_diagram.md#task))
    * Comment: content 1–5,000文字 ([詳細](../er_diagram.md#comment))
    * Like: ユーザー×対象に1回のみ（重複禁止） ([詳細](../er_diagram.md#like))

---

## 5. API規約

* **Base Path**: `/api/v1`
* **一覧**: ページング `?page=1&size=20`、ソート `?sort=createdAt,desc`
* **検索**: クエリパラメータで提供
* **ステータスコード**: `201 Created`（Location付き）、`204 No Content` を積極活用
* **エラー**: 400系はValidation/権限不足、500系はサーバ内部エラー

---

## 6. セキュリティ

* **認証**: Spring Security + JWT（Secure/HttpOnly/SameSite=Strict クッキー）。
* **認可**: Taskの `visibility`（private/team/public）と `UserTeam`（joined, role）に基づき強制（詳細は [ER図設計](../er_diagram.md) の該当セクションを参照）。
* **CORS**: origin 明示。credentials=true 時は origin 固定。
* **CSRF**: stateless API 前提で無効化。Origin検証/レート制限を適用。

---

## 7. ロギング/可観測性

* **構造化ログ**: JSON形式。`requestId`, `userId` を必須項目に。
* **監査ログ**: 重要操作（権限変更、削除、可視性変更）を記録。
* **Actuator**: `/actuator/health`, `/metrics` 有効。

---

## 8. 品質ゲート

* **Spotless + GoogleJavaFormat**: 自動整形。
* **Checkstyle / PMD / SpotBugs**: 静的解析。
* **JaCoCo**: 命令 80% / 分岐 70% を基準値。
* **Conventional Commits**: `feat:`, `fix:`, `test:` など。
* **CI/CD**: GitHub Actions で PR 時に全ゲートを実行。

---

## 9. テスト戦略（Testing Pyramid）

* **Unit**: ドメイン/サービス単位、外部依存はMock。
* **Slice**: `@WebMvcTest`, `@DataJpaTest`。
* **Integration**: Testcontainers(PostgreSQL)でAPI～DB貫通。
* **Contract**: 外部連携が増えたら導入。

### カバレッジ目安

* Unit: ロジック分岐を境界値まで網羅。
* Integration: ユースケース単位のハッピーパス+代表エラーパス。

---

## 10. TDD ガイド

* **Red → Green → Refactor** を厳格に適用。
* テスト命名は Given-When-Then。
* 1ユースケース=1コミット（テスト→実装→リファクタ）。
* PRは小さく（300行程度）保ち、レビュー容易性を優先。

---

## 11. セキュリティ・テスト観点

* 可視性: private/team/public ごとのアクセス制御テスト ([ER図該当箇所](../er_diagram.md#アクセス制御))
* メンバーシップ: `pending/joined/rejected` の遷移テスト ([ER図該当箇所](../er_diagram.md#メンバーシップ遷移))
* 入力: Validationエラーの一貫性（400）
* 認証: Cookie属性（HttpOnly/SameSite）とCORS設定の確認

---

## 12. PR チェックリスト

* [ ] 命名・責務が明確
* [ ] Red→Green→Refactorの順でテスト追加
* [ ] 例外/エラー応答が統一スキーマ
* [ ] 可視性/ロール制御のテストが含まれる
* [ ] ログ・監査の出力ポイントを追加
* [ ] CI（整形/解析/カバレッジ）が通過

---

## 13. 今後の拡張

* **モジュール分割**: `tosk-domain`, `tosk-app`, `tosk-api`, `tosk-infra` への分離
* **通知のリアルタイム化**: SSE/WebSocket
* **検索強化**: 全文検索/保存フィルタ

---

## 14. OpenAPI コード生成ポリシー（Backend視点）

> 目的: API 仕様を唯一のソース・オブ・トゥルースにし、再生成で安全に進化させる。

### 14.1 生成対象と “手書き” の境界

* **生成する**:

    * API I/F（Controller ではなく **インターフェース** もしくはルーティング契約）
    * Request/Response **DTO**、共通スキーマ（ページング、エラー、列挙型）
    * API クライアント（フロント/他サービス用）
* **手書きにする**:

    * ドメインモデル、アプリケーションサービス、ビジネスポリシー
    * リポジトリ実装、トランザクション制御、セキュリティ
    * Controller（実装クラス）：生成 I/F を **実装** する形で手書き

### 14.2 ディレクトリ/モジュール方針

* スキーマ: `doc/spec/api_spec_internal.yaml`（既存の場所）
* 生成物の置き場所（例）:

    * Backend: `generated/backend-openapi/`（**コミットする**。手を入れない）
    * Frontend: `generated/frontend-openapi/`
* 将来のマルチモジュール化:

    * `tosk-api`（OpenAPI依存 + 生成 DTO/IF を公開）
    * `tosk-app`（ユースケース/ポリシー）
    * `tosk-infra`（DB/外部）

### 14.3 運用ルール

* **再生成は常に上書き**。生成物は編集禁止（PRレビューで検知）。
* **テンプレートカスタム**は最小限（命名やrecord対応など）。
* **バージョニング**: API 破壊変更は `/v2` など新バージョンに分岐。旧版は一定期間互換運用。
* **CI**: `api_spec_internal.yaml` 変更時に自動生成＋ビルド。差分を PR に表示。
* **契約テスト**: 生成 I/F と実装の乖離検出（Spring `@WebMvcTest` + スナップショット、もしくは Spring Cloud Contract）。

### 14.4 設計指針（スキーマ）

* **日時**: `date-time` は `UTC ISO-8601` 固定。`date` は日付のみ。
* **ID**: 文字列 `uuid`。
* **Enum**: スキーマに列挙値を明記（`priority`, `visibility`, `role`, `status`, `notification.type`）。
* **ページング**: `Page<T>` 相当の共通スキーマを定義（`items`, `page`, `size`, `total`）。
* **エラー**: `{ code, message, details, debugId }` を共通コンポーネントに。
* **参照整合**: ER に存在する関連以外は定義しない（拡張は将来の `v2` で）。

### 14.5 生成ツール・設定（方針）

* **openapi-generator** を採用（Gradle/Maven プラグイン）。
* Java 設定例の方針:

    * `useSpringBoot3=true`, `dateLibrary=java8`, `interfaceOnly=true`, `useTags=true`
    * `serializationLibrary=jackson`
    * `enumPropertyNaming=UPPERCASE`（プロジェクト方針に合わせる）
* フロントは `typescript-fetch`（もしくは `typescript-axios`）で生成。

### 14.6 レビュー/チェックリスト

* [ ] 仕様の破壊変更は `vN` 増分か？
* [ ] DTO に不要な内部情報（ドメインID以外の内部フィールド）が漏れていないか？
* [ ] 列挙・日付・ページング・エラーが共通コンポーネントを参照しているか？
* [ ] 再生成でコンパイルが通るか（手書き実装が I/F を満たすか）？
* [ ] 生成物へ手編集が混入していないか？

---

## 15. 付録（参考）

* 生成物は **黒箱**として扱い、手を入れず、差分は常にスキーマ側で吸収する。
* 迷ったら「**仕様が源泉**、コードは **仕様の派生物**」の原則に立ち戻る。
