#!/bin/bash

# JWT用JWKS生成スクリプト
# 使用方法: ./scripts/generate-jwks.sh

set -e

KEYS_DIR="./keys"
PRIVATE_PEM="$KEYS_DIR/jwt-private.pem"
PUBLIC_PEM="$KEYS_DIR/jwt-public.pem"
PRIVATE_JWKS="$KEYS_DIR/private-jwks.json"
PUBLIC_JWKS="$KEYS_DIR/public-jwks.json"

echo "🔐 JWT用JWKS鍵ペアを生成します..."

# keysディレクトリを作成
mkdir -p "$KEYS_DIR"

# kid生成（tosk-YYYYMMDD-HHmm形式）
KID="tosk-$(date +%Y%m%d-%H%M)"
echo "📝 Key ID: $KID"

# 秘密鍵生成 (RSA 2048bit)
echo "📝 秘密鍵を生成中..."
openssl genrsa -out temp_rsa_key.pem 2048

# PKCS#8形式に変換
echo "📝 PKCS#8形式に変換中..."
openssl pkcs8 -topk8 -inform PEM -outform PEM -nocrypt -in temp_rsa_key.pem -out "$PRIVATE_PEM"

# 公開鍵生成
echo "📝 公開鍵を生成中..."
openssl rsa -in temp_rsa_key.pem -pubout -out "$PUBLIC_PEM"

# 一時ファイル削除
rm temp_rsa_key.pem

echo "📝 JWKS形式に変換中..."

# Node.jsを使ってJWKS変換（jose-utilsライブラリ使用想定）
node - << 'EOF'
const fs = require('fs');
const { importSPKI, importPKCS8, exportJWK } = require('jose');

async function generateJWKS() {
  try {
    // PEMファイル読み込み
    const publicPem = fs.readFileSync('./keys/jwt-public.pem', 'utf8');
    const privatePem = fs.readFileSync('./keys/jwt-private.pem', 'utf8');
    
    // 公開鍵をJWKに変換
    const publicKey = await importSPKI(publicPem, 'RS256');
    const publicJWK = await exportJWK(publicKey);
    publicJWK.kid = process.env.KID;
    publicJWK.alg = 'RS256';
    publicJWK.use = 'sig';
    
    // 秘密鍵をJWKに変換
    const privateKey = await importPKCS8(privatePem, 'RS256');
    const privateJWK = await exportJWK(privateKey);
    privateJWK.kid = process.env.KID;
    privateJWK.alg = 'RS256';
    privateJWK.use = 'sig';
    
    // private JWKS作成
    const privateJWKS = {
      keys: [privateJWK]
    };
    
    // public JWKS作成
    const publicJWKS = {
      keys: [publicJWK]
    };
    
    // ファイル出力
    fs.writeFileSync('./keys/private-jwks.json', JSON.stringify(privateJWKS, null, 2));
    fs.writeFileSync('./keys/public-jwks.json', JSON.stringify(publicJWKS, null, 2));
    
    console.log('✅ JWKS生成完了!');
  } catch (error) {
    console.error('❌ JWKS変換エラー:', error.message);
    process.exit(1);
  }
}

generateJWKS();
EOF

# jose パッケージがない場合の代替処理（Pythonを使用）
if [ $? -ne 0 ]; then
  echo "⚠️ Node.js/jose が利用できません。Python版を使用します..."
  
  python3 - << EOF
import json
import base64
from cryptography.hazmat.primitives import serialization
from cryptography.hazmat.primitives.asymmetric import rsa
import os

def b64url_encode(data):
    return base64.urlsafe_b64encode(data).rstrip(b'=').decode('utf-8')

def pem_to_jwk(private_pem_path, public_pem_path, kid):
    # 秘密鍵読み込み
    with open(private_pem_path, 'rb') as f:
        private_key = serialization.load_pem_private_key(f.read(), password=None)
    
    # 公開鍵読み込み
    with open(public_pem_path, 'rb') as f:
        public_key = serialization.load_pem_public_key(f.read())
    
    # RSA数値取得
    private_numbers = private_key.private_numbers()
    public_numbers = private_numbers.public_numbers
    
    # JWK形式に変換
    private_jwk = {
        "kty": "RSA",
        "kid": kid,
        "alg": "RS256",
        "use": "sig",
        "n": b64url_encode(public_numbers.n.to_bytes((public_numbers.n.bit_length() + 7) // 8, 'big')),
        "e": b64url_encode(public_numbers.e.to_bytes((public_numbers.e.bit_length() + 7) // 8, 'big')),
        "d": b64url_encode(private_numbers.private_exponent.to_bytes((private_numbers.private_exponent.bit_length() + 7) // 8, 'big')),
        "p": b64url_encode(private_numbers.p.to_bytes((private_numbers.p.bit_length() + 7) // 8, 'big')),
        "q": b64url_encode(private_numbers.q.to_bytes((private_numbers.q.bit_length() + 7) // 8, 'big')),
        "dp": b64url_encode(private_numbers.dmp1.to_bytes((private_numbers.dmp1.bit_length() + 7) // 8, 'big')),
        "dq": b64url_encode(private_numbers.dmq1.to_bytes((private_numbers.dmq1.bit_length() + 7) // 8, 'big')),
        "qi": b64url_encode(private_numbers.iqmp.to_bytes((private_numbers.iqmp.bit_length() + 7) // 8, 'big'))
    }
    
    public_jwk = {
        "kty": "RSA",
        "kid": kid,
        "alg": "RS256",
        "use": "sig",
        "n": private_jwk["n"],
        "e": private_jwk["e"]
    }
    
    return private_jwk, public_jwk

kid = "$KID"
private_jwk, public_jwk = pem_to_jwk("./keys/jwt-private.pem", "./keys/jwt-public.pem", kid)

private_jwks = {"keys": [private_jwk]}
public_jwks = {"keys": [public_jwk]}

with open("./keys/private-jwks.json", "w") as f:
    json.dump(private_jwks, f, indent=2)

with open("./keys/public-jwks.json", "w") as f:
    json.dump(public_jwks, f, indent=2)

print("✅ Python版JWKS生成完了!")
EOF
fi

# 権限を制限
chmod 600 "$PRIVATE_PEM" "$PRIVATE_JWKS"
chmod 644 "$PUBLIC_PEM" "$PUBLIC_JWKS"

echo ""
echo "✅ 鍵ペア生成完了！"
echo ""
echo "生成されたファイル:"
echo "  秘密鍵 PEM: $PRIVATE_PEM"
echo "  公開鍵 PEM: $PUBLIC_PEM"
echo "  Private JWKS: $PRIVATE_JWKS"
echo "  Public JWKS: $PUBLIC_JWKS"
echo "  Key ID: $KID"
echo ""
echo "⚠️  重要な注意事項:"
echo "  - Private JWKSは絶対に外部に漏らさないでください"
echo "  - keysディレクトリは.gitignoreに追加されています"
echo ""
echo "🔒 ファイル権限を設定しました"
echo ""
echo "📋 環境変数設定（JWKS版）:"
echo ""

# Base64エンコード（改行なし）
PRIVATE_JWKS_B64=$(base64 -i "$PRIVATE_JWKS" | tr -d '\n')
echo "# .env ファイルに以下を追加:"
echo "APP_JWT_PRIVATE_JWKS_B64=\"$PRIVATE_JWKS_B64\""
echo ""
echo "# または個別PEMファイル使用（開発用）:"
echo "JWT_PRIVATE_KEY=\"\$(cat $PRIVATE_PEM)\""
echo "JWT_PUBLIC_KEY=\"\$(cat $PUBLIC_PEM)\""