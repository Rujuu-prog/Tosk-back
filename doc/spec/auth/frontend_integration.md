# フロントエンド統合ガイド（Auth）

本書は Web フロント（SPA/SSR）から本バックエンドの認証 API を安全に利用するための手順と実装例をまとめたものです。図は概念を掴むためのもの（Mermaid）。

> **⚠️ 重要**: このドキュメントは実装と同期されており、エンドポイントパス、エラーコード、レスポンス形式はすべて実際のコードに基づいています。

## 1. 事前準備（設定）

- バックエンド URL とフロントのオリジン（Origin）を決める
  - 例）Backend: `https://api.example.com` / Front: `https://app.example.com`（別ドメイン）
- CORS（開発時）
  - `local` or `docker` プロファイル時のみ有効（`CorsConfig`）。
  - プロパティ `app.cors.allowed-origins` にフロントのオリジンを設定（カンマ区切り可）。
- Cookie 設定（JWT）
  - `app.jwt.cookieSameSite`: 同一サイト動作に合わせて `Strict`/`Lax`/`None` を選択。
    - 別ドメイン間で Cookie を送受信する場合は `None` を必須（HTTPS かつ Secure=true）。
  - `app.jwt.cookieSecure`: 本番では `true`（HTTPS 前提）。開発で HTTP の場合は `false` にする。
  - `app.jwt.cookieDomain`: 共有ドメインを指定する場合のみ設定（例: `.example.com`）。
  - `app.jwt.accessCookiePath`（初期値 `/`）、`app.jwt.refreshCookiePath`（初期値 `/auth`）で Cookie Path をプロパティ化。
    - ブラウザは「リクエストパスが Cookie の Path を前方一致」した時のみ送信します。
    - 例）バックエンドのリフレッシュが `/api/auth/refresh` なら `refreshCookiePath=/api/auth` を推奨。
- 鍵/JWKS（本番）
  - `APP_JWT_PRIVATE_JWKS_B64` に秘密 JWKS（先頭が active key）を Base64 で設定。
  - 公開鍵は `/.well-known/jwks.json` で配布（ETag/Cache-Control 付与）。
- メール
  - 開発: `app.mail.provider=logging`（ログ出力）
  - 本番: `app.mail.provider=resend`（`RESEND_API_KEY` 必須）または `smtp`（`spring.mail.*`）。

## 2. フロントの HTTP クライアント設定

Cookie に AT/RT（HttpOnly）を格納するため、必ず「クッキー同送」を有効化します。

**重要**: デフォルトのCookie設定は以下の通りです：
- Access Token: `AT` （Path: `/`）
- Refresh Token: `RT` （Path: `/auth`）
- SameSite: `Strict` （別ドメインの場合は `None` に変更必須）
- Secure: `true` （HTTP環境では `false` に設定）

例: Axios

```ts
import axios from 'axios';

export const api = axios.create({
  baseURL: process.env.NEXT_PUBLIC_API_BASE_URL, // 例: https://api.example.com
  withCredentials: true, // ← 必須（Cookie を送受信）
  headers: { 'Content-Type': 'application/json' },
});
```

Fetch の場合:

```ts
await fetch(`${API_BASE}/api/auth/login`, {
  method: 'POST',
  credentials: 'include', // ← 必須
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ email, password }),
});
```

## 3. API エンドポイント一覧

### 認証フロー
- `POST /api/auth/signup` - 新規登録
- `POST /api/auth/login` - ログイン
- `POST /api/auth/refresh` - トークンリフレッシュ
- `POST /api/auth/logout` - ログアウト
- `GET /api/auth/me` - 現在のユーザー情報取得

### メール認証
- `POST /api/auth/verification/start` - メール認証開始
- `POST /api/auth/verification/confirm?token=...` - メール認証確定

### パスワードリセット
- `POST /api/auth/password/reset/start?email=...` - パスワードリセット開始
- `POST /api/auth/password/reset/confirm?token=...&password=...` - パスワードリセット確定

### セッション管理
- `GET /api/auth/sessions` - セッション一覧
- `GET /api/auth/sessions/{sid}` - セッション詳細
- `DELETE /api/auth/sessions/{sid}` - 単一セッション削除
- `DELETE /api/auth/sessions?keepCurrent=true|false` - 全セッション削除

### JWKS
- `GET /.well-known/jwks.json` - 公開鍵セット（ETag/Cache-Control 対応）

## 4. フロー（サインアップ/ログイン/リフレッシュ/ログアウト）

### 4.1 サインアップ

```mermaid
sequenceDiagram
  participant B as Browser
  participant FE as Front App
  participant BE as Backend

  FE->>BE: POST /api/auth/signup (email, username, displayName, password)
  BE-->>B: Set-Cookie: AT(HttpOnly), RT(HttpOnly)
  BE-->>FE: 201 + User(JSON)
```

ポイント:
- レスポンスで Set-Cookie される AT/RT は HttpOnly のため、JS から読めません。クッキーはブラウザが保持し、以後のリクエストで自動送信されます。

### 4.2 ログイン

```mermaid
sequenceDiagram
  FE->>BE: POST /api/auth/login (email, password)
  BE-->>B: Set-Cookie: AT, RT（更新）
  BE-->>FE: 200 + User(JSON)
```

### 4.3 認証付き API 呼び出し

```mermaid
sequenceDiagram
  FE->>BE: GET /api/auth/me (Cookie: AT)
  BE-->>FE: 200 + User(JSON)
```

### 4.4 AT 失効時のリフレッシュ

```mermaid
sequenceDiagram
  FE->>BE: 任意 API（Cookie: AT 期限切れ）
  BE-->>FE: 401 JSON（リダイレクトなし）
  FE->>BE: POST /api/auth/refresh (Cookie: RT)
  alt 正常ローテーション
    BE-->>B: Set-Cookie: 新AT/新RT
    BE-->>FE: 200 {status: rotated}
    FE->>BE: 失敗した API を再試行
  else RT 再利用検知
    BE-->>FE: 409 TOKEN_REUSE_DETECTED
    FE->>FE: セッション強制終了（ログアウト UI）
  else RT 不足/不正
    BE-->>FE: 401 JSON
    FE->>FE: ログイン画面へ誘導
  end
```

実装例（Axios インターセプタ）:

```ts
api.interceptors.response.use(
  r => r,
  async (error) => {
    const { config, response } = error;
    if (!response) throw error;
    if (response.status === 401 && !config.__retried) {
      try {
        await api.post('/api/auth/refresh');
        config.__retried = true;
        return api.request(config);
      } catch (e: any) {
        if (e?.response?.status === 409) {
          // TOKEN_REUSE_DETECTED
          // → 強制ログアウト/全セッションクリア
        }
        // 401/その他 → ログインへ
      }
    }
    throw error;
  }
);
```

注意（重要）:
- RT Cookie Path は `app.jwt.refreshCookiePath` で変更できます。バックエンドのエンドポイントに合わせて設定してください。

### 4.5 ログアウト

```mermaid
sequenceDiagram
  FE->>BE: POST /api/auth/logout (Cookie: RT 任意)
  BE-->>B: Set-Cookie: AT/RT=削除
  BE-->>FE: 204 No Content
```

## 5. エラーハンドリング

### エラーレスポンス形式

すべてのエラーレスポンスは以下の形式で返されます：

```json
{
  "errorCode": "ERROR_CODE_NAME",
  "message": "Human readable error message"
}
```

### 主要なエラーコード

| エラーコード | HTTPステータス | 説明 |
| ----------- | ------------- | ---- |
| `VALIDATION_ERROR` | 400 | バリデーションエラー |
| `AUTH_INVALID_CREDENTIALS` | 401 | 認証失敗（ログイン情報不正） |
| `TOKEN_INVALID` | 401 | トークンが無効 |
| `TOKEN_MISSING` | 401 | トークンが不足 |
| `TOKEN_REUSE_DETECTED` | 409 | トークン再利用検知（セキュリティ違反） |
| `AUTH_RATE_LIMIT` | 429 | レート制限に抵触 |
| `NOT_FOUND` | 404 | リソースが見つからない |

### エラーハンドリングの実装例

```ts
// 409 TOKEN_REUSE_DETECTED の場合は強制ログアウト
if (error.response?.status === 409 && 
    error.response?.data?.errorCode === 'TOKEN_REUSE_DETECTED') {
  // 全セッションクリア + ログイン画面へ誘導
  await forceLogout();
  redirectToLogin();
  return;
}

// 401の場合はリフレッシュ試行
if (error.response?.status === 401) {
  // リフレッシュロジック...
}
```

## 6. メールフロー（任意）

- メール認証開始: `POST /api/auth/verification/start`（AT 必須）
- メール認証確定: `POST /api/auth/verification/confirm?token=...`
- パスワードリセット開始: `POST /api/auth/password/reset/start?email=...`（常に 202）
- パスワードリセット確定: `POST /api/auth/password/reset/confirm?token=...&password=...`

### 6.1 パスワードリセット図解

```mermaid
sequenceDiagram
  participant FE as Front App
  participant BE as Backend
  participant M as Mail

  FE->>BE: POST /api/auth/password/reset/start?email=foo@example.com
  BE-->>FE: 202 Accepted (常に)
  BE-->>M: Send mail (reset link with token)
  Note over FE,M: ユーザはメールのリンクをクリック
  FE->>BE: POST /api/auth/password/reset/confirm?token=...&password=NewPwd
  alt token 有効
    BE-->>FE: 200 OK (password_reset)
    BE-->>FE: （任意）既存セッション全失効（tokenVersion++）
  else token 無効/期限切れ
    BE-->>FE: 400 Bad Request
  end
```

## 7. セッション管理

- 一覧: `GET /api/auth/sessions`
- 詳細: `GET /api/auth/sessions/{sid}`
- 1件失効: `DELETE /api/auth/sessions/{sid}`
- 全失効: `DELETE /api/auth/sessions?keepCurrent=true|false`

## 8. よくある落とし穴

- withCredentials/credentials=include を忘れる → Cookie が送受信されない
- SameSite が `Strict` のまま別ドメインで使う → Cookie が送られない
- Secure=false で `SameSite=None` を使う → ブラウザが拒否（`None` は Secure 必須）
- RT の Cookie Path とエンドポイントパスが不一致 → RT が送られない（要プロキシまたはサーバ改善）

## 9. 推奨プロキシ設定（例: nginx）

```nginx
location /auth/ {
  proxy_set_header Host $host;
  proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
  proxy_pass http://backend:8080/api/auth/;
}
```

フロントは `/auth/refresh` 等を呼び出し、ブラウザは Path=/auth の RT Cookie を送信します。

## 10. セキュリティ考慮事項

### レート制限
- ログインエンドポイント（`POST /api/auth/login`）は IP + メールアドレスでレート制限されています
- 制限に達すると `429 Too Many Requests` が返され、`AUTH_RATE_LIMIT` エラーコードが設定されます

### トークンローテーション
- リフレッシュトークンは使用のたびに新しいものに更新されます（ローテーション）
- 古いリフレッシュトークンの再利用が検知されると、全セッションが無効化されます
- この場合、`409 Conflict` で `TOKEN_REUSE_DETECTED` エラーが返されます

### セッションベースの無効化
- パスワード変更時、`tokenVersion` がインクリメントされ、既存の全トークンが無効化されます
- セッション単位での無効化も可能です（`DELETE /api/auth/sessions/{sid}`）

### JWKS キーローテーション
- 公開鍵は `/.well-known/jwks.json` で提供され、ETag/Cache-Control でキャッシュされます
- 複数の鍵を含む JWKS をサポートしており、鍵ローテーションに対応しています
- アクティブな鍵は JWKS の最初の鍵として配置されます
