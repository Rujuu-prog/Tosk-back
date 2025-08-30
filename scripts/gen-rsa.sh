#!/usr/bin/env bash
set -euo pipefail

# Generate RSA key pair for local development under dev/certs
DIR="dev/certs"
mkdir -p "$DIR"

PRIV="$DIR/private_key.pem"
PUB="$DIR/public_key.pem"

if ! command -v openssl >/dev/null 2>&1; then
  echo "openssl not found. Please install openssl." >&2
  exit 1
fi

echo "Generating RSA 2048 private key..."
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out "$PRIV"
chmod 600 "$PRIV"

echo "Extracting public key..."
openssl rsa -in "$PRIV" -pubout -out "$PUB"

cat <<EOF
Done.
Set the following environment variables for local run:

  export APP_JWT_PRIVATE_KEY_PATH=$PRIV
  export APP_JWT_PUBLIC_KEY_PATH=$PUB

Then run:
  SPRING_PROFILES_ACTIVE=local ./gradlew bootRun
EOF

