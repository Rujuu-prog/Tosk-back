#!/bin/bash

# シンプルなJWT用JWKS生成スクリプト
# 使用方法: ./scripts/generate-jwks-simple.sh

set -e

KEYS_DIR="./keys"
PRIVATE_PEM="$KEYS_DIR/jwt-private.pem"
PUBLIC_PEM="$KEYS_DIR/jwt-public.pem"
PRIVATE_JWKS="$KEYS_DIR/private-jwks.json"

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

echo "📝 OpenSSLでJWKS形式に変換中..."

# OpenSSLとPythonを使ってJWKS作成
python3 << EOF
import json
import base64
import subprocess
import re
import os

def b64url_encode(data):
    """Base64URL encoding without padding"""
    return base64.urlsafe_b64encode(data).rstrip(b'=').decode('utf-8')

def extract_rsa_components(private_pem_path):
    """OpenSSLを使ってRSA数値を取得"""
    # 秘密鍵から数値取得（OpenSSL text format）
    result = subprocess.run(['openssl', 'rsa', '-in', private_pem_path, '-text', '-noout'], 
                          capture_output=True, text=True)
    if result.returncode != 0:
        raise Exception(f"OpenSSL error: {result.stderr}")
    
    text_output = result.stdout
    
    # 正規表現で各数値を抽出
    def extract_hex_value(param_name, text):
        pattern = rf'{param_name}:\s*\n((?:\s+[0-9a-f:]+\s*\n)*)'
        match = re.search(pattern, text, re.IGNORECASE)
        if not match:
            raise Exception(f"Could not find {param_name}")
        hex_str = re.sub(r'[:\s\n]', '', match.group(1))
        return bytes.fromhex(hex_str)
    
    # 各RSA数値を取得
    n = extract_hex_value('modulus', text_output)
    e = extract_hex_value('publicExponent', text_output) 
    d = extract_hex_value('privateExponent', text_output)
    p = extract_hex_value('prime1', text_output)
    q = extract_hex_value('prime2', text_output)
    dp = extract_hex_value('exponent1', text_output)
    dq = extract_hex_value('exponent2', text_output)
    qi = extract_hex_value('coefficient', text_output)
    
    return n, e, d, p, q, dp, dq, qi

try:
    kid = "$KID"
    n, e, d, p, q, dp, dq, qi = extract_rsa_components("./keys/jwt-private.pem")
    
    # Private JWK作成
    private_jwk = {
        "kty": "RSA",
        "kid": kid,
        "alg": "RS256", 
        "use": "sig",
        "n": b64url_encode(n),
        "e": b64url_encode(e),
        "d": b64url_encode(d),
        "p": b64url_encode(p),
        "q": b64url_encode(q),
        "dp": b64url_encode(dp),
        "dq": b64url_encode(dq),
        "qi": b64url_encode(qi)
    }
    
    # Public JWK作成（n, eのみ）
    public_jwk = {
        "kty": "RSA",
        "kid": kid,
        "alg": "RS256",
        "use": "sig", 
        "n": private_jwk["n"],
        "e": private_jwk["e"]
    }
    
    # JWKS作成
    private_jwks = {"keys": [private_jwk]}
    public_jwks = {"keys": [public_jwk]}
    
    # ファイル出力
    with open("./keys/private-jwks.json", "w") as f:
        json.dump(private_jwks, f, indent=2)
        
    with open("./keys/public-jwks.json", "w") as f:
        json.dump(public_jwks, f, indent=2)
    
    print("✅ JWKS生成完了!")
    
except Exception as e:
    print(f"❌ エラー: {e}")
    exit(1)
EOF

if [ $? -ne 0 ]; then
    echo "❌ JWKS生成に失敗しました"
    exit 1
fi

# 権限を制限
chmod 600 "$PRIVATE_PEM" "$PRIVATE_JWKS"
chmod 644 "$PUBLIC_PEM" "$KEYS_DIR/public-jwks.json"

echo ""
echo "✅ 鍵ペア生成完了！"
echo ""
echo "生成されたファイル:"
echo "  Private JWKS: $PRIVATE_JWKS"
echo "  Public JWKS: $KEYS_DIR/public-jwks.json"  
echo "  Key ID: $KID"
echo ""
echo "⚠️  重要な注意事項:"
echo "  - Private JWKSは絶対に外部に漏らさないでください"
echo ""

# Base64エンコード（改行なし）
if [ -f "$PRIVATE_JWKS" ]; then
    PRIVATE_JWKS_B64=$(base64 -i "$PRIVATE_JWKS" | tr -d '\n')
    echo "📋 環境変数設定:"
    echo ""
    echo "# .env ファイルに以下を追加:"
    echo "APP_JWT_PRIVATE_JWKS_B64=\"$PRIVATE_JWKS_B64\""
    echo ""
else
    echo "❌ private-jwks.json の生成に失敗しました"
fi