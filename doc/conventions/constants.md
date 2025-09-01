# 定数と設定の管理方針

本プロジェクトでは、ビルド時に固定される定数（コード識別子）と、環境で変わり得る設定値（プロパティ）を分離して管理する。

## 1. 定数（コード識別子）

- ファイル: `src/main/java/com/tosk/app/common/ApiConstants.java`
- 目的: APIで横断的に用いる識別子・キーを集中管理し、実装/テスト/ドキュメントの整合性を高める。
- 主な項目:
  - クッキー名: `COOKIE_AT`, `COOKIE_RT`
  - クエリ名: `PARAM_TOKEN`, `PARAM_EMAIL`
  - エラーコード: `ERR_VALIDATION`, `ERR_UNAUTHORIZED`, `ERR_RATE_LIMIT`, `ERR_TOKEN_INVALID`, `ERR_TOKEN_MISSING`, `ERR_NOT_FOUND`
  - 監査イベント: `EVT_SIGNUP_SUCCESS` 等

利用指針:
- 例外ハンドラ・コントローラでのエラー応答は必ず `ApiConstants` に定義された `errorCode` を使用する。
- 監査ログの `event` も `ApiConstants` を使用し、自由文字列を避ける。

## 2. 設定（プロパティ）

- ファイル: `src/main/java/com/tosk/app/common/AppProperties.java`
- プレフィックス: `app.*`
- 主な項目:
  - `app.frontend-base-url`: メール内リンク等のベースURL
  - `app.mail.from`, `app.mail.subject-prefix`: メール送信の共通設定
  - `app.security.login.max-attempts`, `app.security.login.window-seconds`: ログインのレート制限

利用指針:
- コード内でハードコードせず、`AppProperties` 経由で参照する。
- 本番は環境変数/CapRoverのApp Configsで上書きする。

## 3. Rate Limit（Redisを使わない方針）

- インフラ設計（`doc/spec/infra.md`）に従い、Redis等の外部KVSは採用しない。
- エッジ: CloudflareのRateLimit機能でグローバルなレイヤの制御。
- アプリ: `LoginRateLimiter` によるインメモリ制御（単機VPS前提）。必要に応じてプロパティで閾値調整。

## 4. ドキュメント・OpenAPI

- OpenAPIの `ErrorResponse.errorCode` は `ApiConstants` の値と一致させる（列挙を同期）。
- 新規API追加時は、まず `ApiConstants` / `AppProperties` が必要か検討し、必要な場合は先に定義する。

