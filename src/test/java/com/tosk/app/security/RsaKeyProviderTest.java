package com.tosk.app.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import org.junit.jupiter.api.Test;

class RsaKeyProviderTest {
  @Test
  void loadsKeysFromPemInProperties() throws Exception {
    // generate a keypair
    KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
    kpg.initialize(2048);
    KeyPair kp = kpg.generateKeyPair();
    // DER encodings
    byte[] privDer = kp.getPrivate().getEncoded(); // PKCS#8
    byte[] pubDer = kp.getPublic().getEncoded(); // X.509
    String privPem =
        "-----BEGIN PRIVATE KEY-----\n"
            + Base64.getMimeEncoder(64, new byte[] {'\n'}).encodeToString(privDer)
            + "\n-----END PRIVATE KEY-----\n";
    String pubPem =
        "-----BEGIN PUBLIC KEY-----\n"
            + Base64.getMimeEncoder(64, new byte[] {'\n'}).encodeToString(pubDer)
            + "\n-----END PUBLIC KEY-----\n";

    JwtProperties props = new JwtProperties();
    props.setPrivateKeyPem(privPem);
    props.setPublicKeyPem(pubPem);

    RsaKeyProvider provider = new RsaKeyProvider(props);
    assertThat(provider.getPrivateKey()).isNotNull();
    assertThat(provider.getPublicKey()).isNotNull();
    assertThat(provider.getKeyId()).isNotBlank();
    assertThat(provider.getPublicJwkSet().toJSONObject()).isNotEmpty();
  }
}
