package com.tosk.app.util;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

public class JwksGenerator {
  
  // Base64URL エンコーディング（JWK仕様）
  private static String base64UrlEncode(BigInteger value) {
    byte[] bytes = value.toByteArray();
    // 先頭が0の場合は除去（符号ビット対応）
    if (bytes[0] == 0 && bytes.length > 1) {
      byte[] tmp = new byte[bytes.length - 1];
      System.arraycopy(bytes, 1, tmp, 0, tmp.length);
      bytes = tmp;
    }
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }
  
  public static void main(String[] args) throws Exception {
    System.out.println("🔐 JWT用JWKS鍵ペアを生成します...");
    
    // kid生成（tosk-YYYYMMDD-HHmm形式）
    String kid = "tosk-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmm"));
    System.out.println("📝 Key ID: " + kid);
    
    // RSA鍵ペア生成
    System.out.println("📝 RSA鍵ペア生成中...");
    KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
    kpg.initialize(2048);
    KeyPair keyPair = kpg.generateKeyPair();
    
    RSAPrivateCrtKey privateKey = (RSAPrivateCrtKey) keyPair.getPrivate();
    RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
    
    // 手動でJWK JSONを構築（完全な秘密鍵情報付き）
    String privateJwk = String.format(
        "{\"kty\":\"RSA\",\"e\":\"%s\",\"kid\":\"%s\",\"alg\":\"RS256\",\"use\":\"sig\"," +
        "\"n\":\"%s\",\"d\":\"%s\",\"p\":\"%s\",\"q\":\"%s\",\"dp\":\"%s\",\"dq\":\"%s\",\"qi\":\"%s\"}",
        base64UrlEncode(publicKey.getPublicExponent()),
        kid,
        base64UrlEncode(publicKey.getModulus()),
        base64UrlEncode(privateKey.getPrivateExponent()),
        base64UrlEncode(privateKey.getPrimeP()),
        base64UrlEncode(privateKey.getPrimeQ()),
        base64UrlEncode(privateKey.getPrimeExponentP()),
        base64UrlEncode(privateKey.getPrimeExponentQ()),
        base64UrlEncode(privateKey.getCrtCoefficient())
    );
    
    String publicJwk = String.format(
        "{\"kty\":\"RSA\",\"e\":\"%s\",\"kid\":\"%s\",\"alg\":\"RS256\",\"use\":\"sig\",\"n\":\"%s\"}",
        base64UrlEncode(publicKey.getPublicExponent()),
        kid,
        base64UrlEncode(publicKey.getModulus())
    );
    
    String privateJwksJson = String.format("{\"keys\":[%s]}", privateJwk);
    String publicJwksJson = String.format("{\"keys\":[%s]}", publicJwk);
    
    // keysディレクトリ作成
    Path keysDir = Paths.get("keys");
    Files.createDirectories(keysDir);
    
    // ファイル出力
    Files.writeString(keysDir.resolve("private-jwks.json"), privateJwksJson);
    Files.writeString(keysDir.resolve("public-jwks.json"), publicJwksJson);
    
    // Base64エンコード（改行なし・秘密鍵込み）
    String privateJwksB64 = Base64.getEncoder().encodeToString(
        privateJwksJson.getBytes(StandardCharsets.UTF_8)
    );
    
    System.out.println("📝 DEBUG: Private JWKS contains 'd' parameter: " + privateJwksJson.contains("\"d\":"));
    
    System.out.println("");
    System.out.println("✅ 鍵ペア生成完了！");
    System.out.println("");
    System.out.println("生成されたファイル:");
    System.out.println("  Private JWKS: keys/private-jwks.json");
    System.out.println("  Public JWKS: keys/public-jwks.json");
    System.out.println("  Key ID: " + kid);
    System.out.println("");
    System.out.println("⚠️  重要な注意事項:");
    System.out.println("  - Private JWKSは絶対に外部に漏らさないでください");
    System.out.println("");
    System.out.println("📋 環境変数設定:");
    System.out.println("");
    System.out.println("# .env ファイルに以下を追加または置換:");
    System.out.println("APP_JWT_PRIVATE_JWKS_B64=\"" + privateJwksB64 + "\"");
    System.out.println("");
    System.out.println("# 既存のPEM形式の設定は削除してください:");
    System.out.println("# JWT_PRIVATE_KEY=...");
    System.out.println("# JWT_PUBLIC_KEY=...");
  }
}