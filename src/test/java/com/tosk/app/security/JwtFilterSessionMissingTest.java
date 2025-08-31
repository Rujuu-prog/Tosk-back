package com.tosk.app.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.tosk.app.user.UserEntity;
import com.tosk.app.user.UserRepository;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:tosk;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
      "spring.datasource.driver-class-name=org.h2.Driver",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.flyway.enabled=false"
    })
class JwtFilterSessionMissingTest {
  @Autowired MockMvc mockMvc;
  @Autowired JwtProperties props;
  @Autowired RsaKeyProvider keys;
  @Autowired UserRepository users;

  @Test
  void accessTokenWithUnknownSidReturns401() throws Exception {
    // create user
    UserEntity u = new UserEntity();
    u.setEmail("sid-missing@example.com");
    u.setUsername("sidh");
    u.setDisplayName("Sid H");
    users.save(u);

    // craft AT with valid uid/ver but random sid not in DB
    Instant now = Instant.now();
    JWTClaimsSet claims =
        new JWTClaimsSet.Builder()
            .issuer(props.getIssuer())
            .audience(props.getAudience())
            .issueTime(Date.from(now))
            .expirationTime(Date.from(now.plus(props.getAccessTtl())))
            .jwtID(UUID.randomUUID().toString())
            .claim("uid", u.getId().toString())
            .claim("ver", u.getTokenVersion())
            .claim("sid", UUID.randomUUID().toString())
            .build();
    SignedJWT jwt =
        new SignedJWT(
            new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(keys.getKeyId()).build(), claims);
    jwt.sign(new RSASSASigner(keys.getPrivateKey()));
    String token = jwt.serialize();

    Cookie c = new Cookie(props.getAccessCookie(), token);
    c.setPath("/");
    mockMvc
        .perform(get("/api/auth/me").cookie(c).contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized());
  }
}
