# デプロイメント設定ガイド

## Resendメール設定

### 1. Resend アカウント準備
1. [Resend](https://resend.com/)でアカウント作成
2. ドメインを追加・認証
3. API キーを生成

### 2. 設定方法
以下のいずれかの方法でAPI キーを設定：

#### 方法1: 環境変数（推奨）
```bash
export RESEND_API_KEY="re_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"
```

#### 方法2: application.yml設定
```yaml
app:
  mail:
    provider: resend
    from: "noreply@your-domain.com"
    resend-api-key: "re_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"
```

### 3. 本番環境設定
```yaml
# application-prod.yml
app:
  mail:
    provider: resend
    from: "noreply@your-domain.com"  # 認証済みドメイン使用
```

## JWT RSA鍵ペア設定

### 1. 鍵ペア生成
```bash
# プロジェクトルートで実行
./scripts/generate-jwt-keys.sh
```

### 2. 環境変数設定

#### 開発環境
```bash
# .env ファイル（プロジェクトルート）
JWT_PRIVATE_KEY="$(cat keys/jwt-private.pem)"
JWT_PUBLIC_KEY="$(cat keys/jwt-public.pem)"
```

#### 本番環境（Docker/Kubernetes）
```yaml
# docker-compose.yml 例
environment:
  - JWT_PRIVATE_KEY=${JWT_PRIVATE_KEY}
  - JWT_PUBLIC_KEY=${JWT_PUBLIC_KEY}
```

```yaml
# Kubernetes Secret 例
apiVersion: v1
kind: Secret
metadata:
  name: jwt-keys
type: Opaque
data:
  private-key: <base64-encoded-private-key>
  public-key: <base64-encoded-public-key>
```

### 3. セキュリティ注意事項

#### 重要な原則
- 秘密鍵は絶対に外部に漏らさない
- 鍵ファイルはGitにコミットしない
- 本番環境では鍵をローテーション可能にする
- 権限を最小限に制限（秘密鍵：600、公開鍵：644）

#### 鍵ローテーション手順
1. 新しい鍵ペアを生成
2. 公開鍵を更新（既存トークン検証を継続）
3. 秘密鍵を更新（新しいトークン発行）
4. 古い鍵を無効化

## SSL/TLS証明書設定

### 本番環境要件
- HTTPSが必須（JWT Cookie のセキュア属性）
- `cookie-secure: true` に設定

### 証明書取得方法

#### Let's Encrypt（推奨）
```bash
# nginx + certbot 例
certbot --nginx -d your-domain.com
```

#### CloudFlare SSL
```yaml
# nginx proxy 設定例
server {
    listen 443 ssl;
    server_name your-domain.com;
    
    ssl_certificate /etc/ssl/certs/cloudflare.pem;
    ssl_certificate_key /etc/ssl/private/cloudflare.key;
    
    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Forwarded-Proto https;
    }
}
```

### Spring Boot本番設定
```yaml
# application-prod.yml
server:
  forward-headers-strategy: framework  # X-Forwarded-* ヘッダー処理

app:
  jwt:
    cookie-secure: true
```

## 環境変数一覧

| 変数名 | 説明 | 例 |
|--------|------|-----|
| `RESEND_API_KEY` | Resend API キー | `re_xxx...` |
| `JWT_PRIVATE_KEY` | JWT署名用秘密鍵 | `-----BEGIN PRIVATE KEY-----...` |
| `JWT_PUBLIC_KEY` | JWT検証用公開鍵 | `-----BEGIN PUBLIC KEY-----...` |
| `SPRING_DATASOURCE_URL` | DB接続URL | `jdbc:postgresql://...` |
| `SPRING_PROFILES_ACTIVE` | アクティブプロファイル | `prod` |