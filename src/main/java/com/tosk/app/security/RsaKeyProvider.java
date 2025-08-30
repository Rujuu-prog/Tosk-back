package com.tosk.app.security;

import com.nimbusds.jose.jwk.*;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;
import org.springframework.stereotype.Component;

@Component
public class RsaKeyProvider {
  private final RSAPublicKey publicKey;
  private final RSAPrivateKey privateKey;
  private final String keyId;
  private final JWKSet publicJwkSet;

  public RsaKeyProvider(JwtProperties props) {
    try {
      String jwksB64 = System.getenv("APP_JWT_PRIVATE_JWKS_B64");
      if (jwksB64 != null && !jwksB64.isBlank()) {
        byte[] jsonBytes = Base64.getDecoder().decode(jwksB64);
        String json = new String(jsonBytes);
        JWKSet privateSet = JWKSet.parse(json);
        List<JWK> keys = privateSet.getKeys();
        if (keys.isEmpty()) {
          throw new IllegalStateException("private JWKS is empty");
        }
        RSAKey active = (RSAKey) keys.get(0); // 先頭がactive
        this.privateKey = active.toRSAPrivateKey();
        this.publicKey = active.toRSAPublicKey();
        this.keyId = active.getKeyID();
        // 公開用セット（全鍵の公開部のみ）
        List<JWK> pubKeys = new ArrayList<>();
        for (JWK k : keys) {
          pubKeys.add(k.toPublicJWK());
        }
        this.publicJwkSet = new JWKSet(pubKeys);
      } else if (System.getenv("APP_JWT_PRIVATE_KEY_PATH") != null
          && System.getenv("APP_JWT_PUBLIC_KEY_PATH") != null) {
        String privPath = System.getenv("APP_JWT_PRIVATE_KEY_PATH");
        String pubPath = System.getenv("APP_JWT_PUBLIC_KEY_PATH");
        String privPem = java.nio.file.Files.readString(java.nio.file.Path.of(privPath));
        String pubPem = java.nio.file.Files.readString(java.nio.file.Path.of(pubPath));
        this.privateKey = (RSAPrivateKey) parsePrivateKeyFromPem(privPem);
        this.publicKey = (RSAPublicKey) parsePublicKeyFromPem(pubPem);
        this.keyId = UUID.nameUUIDFromBytes(pubPem.getBytes()).toString();
        RSAKey rsaKey = new RSAKey.Builder(this.publicKey).keyID(this.keyId).build();
        this.publicJwkSet = new JWKSet(rsaKey);
      } else if (props.getPrivateKeyPem() != null && props.getPublicKeyPem() != null) {
        this.privateKey = (RSAPrivateKey) parsePrivateKeyFromPem(props.getPrivateKeyPem());
        this.publicKey = (RSAPublicKey) parsePublicKeyFromPem(props.getPublicKeyPem());
        this.keyId = UUID.nameUUIDFromBytes(props.getPublicKeyPem().getBytes()).toString();
        RSAKey rsaKey = new RSAKey.Builder(this.publicKey).keyID(this.keyId).build();
        this.publicJwkSet = new JWKSet(rsaKey);
      } else {
        // 非prodプロファイルでは起動用に一時鍵を生成
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair kp = kpg.generateKeyPair();
        this.privateKey = (RSAPrivateKey) kp.getPrivate();
        this.publicKey = (RSAPublicKey) kp.getPublic();
        this.keyId = UUID.randomUUID().toString();
        RSAKey rsaKey = new RSAKey.Builder(this.publicKey).keyID(this.keyId).build();
        this.publicJwkSet = new JWKSet(rsaKey);
      }
    } catch (Exception e) {
      throw new IllegalStateException("RSA鍵の初期化に失敗しました", e);
    }
  }

  public RSAPublicKey getPublicKey() {
    return publicKey;
  }

  public RSAPrivateKey getPrivateKey() {
    return privateKey;
  }

  public String getKeyId() {
    return keyId;
  }

  public JWKSet getPublicJwkSet() {
    return publicJwkSet;
  }

  private static PublicKey parsePublicKeyFromPem(String pem) throws Exception {
    String c =
        pem.replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replaceAll("\n", "")
            .trim();
    byte[] der = Base64.getDecoder().decode(c);
    X509EncodedKeySpec spec = new X509EncodedKeySpec(der);
    return KeyFactory.getInstance("RSA").generatePublic(spec);
  }

  private static PrivateKey parsePrivateKeyFromPem(String pem) throws Exception {
    String c =
        pem.replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replaceAll("\n", "")
            .trim();
    byte[] der = Base64.getDecoder().decode(c);
    PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(der);
    return KeyFactory.getInstance("RSA").generatePrivate(spec);
  }
}
