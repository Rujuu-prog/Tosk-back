package com.tosk.app.security;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.text.ParseException;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class JwtServiceMismatchTest {
  @Test
  void issuerMismatchCausesParseException() throws Exception {
    JwtProperties p1 = new JwtProperties();
    p1.setIssuer("iss-A");
    p1.setAudience("aud-X");
    RsaKeyProvider k1 = new RsaKeyProvider(p1);
    JwtService s1 = new JwtService(p1, k1);

    JwtProperties p2 = new JwtProperties();
    p2.setIssuer("iss-B"); // mismatch
    p2.setAudience("aud-X");
    // share same key to isolate iss check
    JwtService s2 = new JwtService(p2, k1);

    // build minimal user
    com.tosk.app.user.UserEntity u = new com.tosk.app.user.UserEntity();
    u.setId(java.util.UUID.randomUUID());
    u.setEmail("x@example.com");
    u.setTokenVersion(0);

    String at = s1.issueAccessToken(u);
    assertThrows(ParseException.class, () -> s2.parseAndValidate(at));
  }

  @Test
  void audienceMismatchCausesParseException() throws Exception {
    JwtProperties p1 = new JwtProperties();
    p1.setIssuer("iss-A");
    p1.setAudience("aud-X");
    p1.setAccessTtl(Duration.ofMinutes(5));
    RsaKeyProvider k1 = new RsaKeyProvider(p1);
    JwtService s1 = new JwtService(p1, k1);

    JwtProperties p2 = new JwtProperties();
    p2.setIssuer("iss-A");
    p2.setAudience("aud-Y"); // mismatch
    JwtService s2 = new JwtService(p2, k1);

    com.tosk.app.user.UserEntity u = new com.tosk.app.user.UserEntity();
    u.setId(java.util.UUID.randomUUID());
    u.setEmail("y@example.com");
    u.setTokenVersion(0);

    String at = s1.issueAccessToken(u);
    assertThrows(ParseException.class, () -> s2.parseAndValidate(at));
  }
}
