#!/bin/bash

# JWT用RSA鍵ペア生成スクリプト
# 使用方法: ./scripts/generate-jwt-keys.sh

set -e

KEYS_DIR="./keys"
PRIVATE_KEY_FILE="$KEYS_DIR/jwt-private.pem"
PUBLIC_KEY_FILE="$KEYS_DIR/jwt-public.pem"

echo "🔐 JWT用RSA鍵ペアを生成します..."

# keysディレクトリを作成
mkdir -p "$KEYS_DIR"

# 秘密鍵生成 (RSA 2048bit)
echo "📝 秘密鍵を生成中..."
openssl genrsa -out temp_rsa_key.pem 2048

# PKCS#8形式に変換
echo "📝 PKCS#8形式に変換中..."
openssl pkcs8 -topk8 -inform PEM -outform PEM -nocrypt -in temp_rsa_key.pem -out "$PRIVATE_KEY_FILE"

# 公開鍵生成
echo "📝 公開鍵を生成中..."
openssl rsa -in temp_rsa_key.pem -pubout -out "$PUBLIC_KEY_FILE"

# 一時ファイル削除
rm temp_rsa_key.pem

echo "✅ 鍵ペア生成完了！"
echo ""
echo "生成されたファイル:"
echo "  秘密鍵: $PRIVATE_KEY_FILE"
echo "  公開鍵: $PUBLIC_KEY_FILE"
echo ""
echo "⚠️  重要な注意事項:"
echo "  - 秘密鍵は絶対に外部に漏らさないでください"
echo "  - プロダクション環境では環境変数で管理してください"
echo "  - keysディレクトリは.gitignoreに追加されています"
echo ""

# 権限を制限
chmod 600 "$PRIVATE_KEY_FILE"
chmod 644 "$PUBLIC_KEY_FILE"

echo "🔒 ファイル権限を設定しました (秘密鍵: 600, 公開鍵: 644)"
echo ""
echo "📋 .env ファイル設定例:"
echo ""
echo "# .env ファイルに以下を追加:"
echo "JWT_PRIVATE_KEY=\"\$(cat $PRIVATE_KEY_FILE)\""
echo "JWT_PUBLIC_KEY=\"\$(cat $PUBLIC_KEY_FILE)\""
echo ""
echo "# または手動でファイルの内容をコピー:"
echo "JWT_PRIVATE_KEY=\"-----BEGIN PRIVATE KEY-----..."
echo "JWT_PUBLIC_KEY=\"-----BEGIN PUBLIC KEY-----..."