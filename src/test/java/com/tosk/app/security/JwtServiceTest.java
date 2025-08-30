package com.tosk.app.security;

import static org.junit.jupiter.api.Assertions.*;

import com.nimbusds.jwt.JWTClaimsSet;
import com.tosk.app.user.UserEntity;
import java.text.ParseException;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JwtServiceTest {
  private JwtService jwtService;
  private JwtProperties props;

  @BeforeEach
  void setUp() {
    props = new JwtProperties();
    props.setAccessTtl(Duration.ofMinutes(5));
    props.setRefreshTtl(Duration.ofDays(7));
    RsaKeyProvider keys = new RsaKeyProvider(props);
    jwtService = new JwtService(props, keys);
  }

  @Test
  void issueAndParseAccessToken() throws Exception {
    UserEntity u = new UserEntity();
    u.setId(UUID.randomUUID());
    u.setEmail("a@example.com");
    u.setTokenVersion(1);
    String at = jwtService.issueAccessToken(u);
    assertNotNull(at);
    JwtService.ParsedJwt pj = jwtService.parseAndValidate(at);
    JWTClaimsSet c = pj.claims();
    assertEquals(props.getIssuer(), c.getIssuer());
    assertTrue(c.getAudience().contains(props.getAudience()));
    assertEquals("1", c.getClaim("ver").toString());
  }

  @Test
  void signatureInvalidCausesParseException() throws Exception {
    // service under test
    JwtProperties p1 = new JwtProperties();
    RsaKeyProvider k1 = new RsaKeyProvider(p1);
    JwtService s1 = new JwtService(p1, k1);

    // attacker with different key issues token
    JwtProperties p2 = new JwtProperties();
    RsaKeyProvider k2 = new RsaKeyProvider(p2); // different key pair
    JwtService s2 = new JwtService(p2, k2);

    com.tosk.app.user.UserEntity u = new com.tosk.app.user.UserEntity();
    u.setId(java.util.UUID.randomUUID());
    u.setEmail("sig@example.com");
    u.setTokenVersion(0);
    String forged = s2.issueAccessToken(u);
    org.junit.jupiter.api.Assertions.assertThrows(
        java.text.ParseException.class, () -> s1.parseAndValidate(forged));
  }

  @Test
  void invalidIssuerRejected() throws Exception {
    UserEntity u = new UserEntity();
    u.setId(UUID.randomUUID());
    u.setEmail("b@example.com");
    String at = jwtService.issueAccessToken(u);
    props.setIssuer("other-issuer");
    assertThrows(ParseException.class, () -> jwtService.parseAndValidate(at));
  }
}
