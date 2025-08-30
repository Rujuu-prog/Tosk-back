package com.tosk.app.security;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.tosk.app.user.UserEntity;
import java.text.ParseException;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
  private final JwtProperties props;
  private final RsaKeyProvider keys;

  public JwtService(JwtProperties props, RsaKeyProvider keys) {
    this.props = props;
    this.keys = keys;
  }

  public String issueAccessToken(UserEntity user) {
    Instant now = Instant.now();
    Instant exp = now.plus(props.getAccessTtl());
    Map<String, Object> claims = new HashMap<>();
    claims.put("uid", user.getId().toString());
    claims.put("email", user.getEmail());
    claims.put("ver", Optional.ofNullable(user.getTokenVersion()).orElse(0));
    return sign(claims, Date.from(now), Date.from(exp), null);
  }

  public String issueRefreshToken(UserEntity user, String jti) {
    Instant now = Instant.now();
    Instant exp = now.plus(props.getRefreshTtl());
    Map<String, Object> claims = new HashMap<>();
    claims.put("uid", user.getId().toString());
    claims.put("ver", Optional.ofNullable(user.getTokenVersion()).orElse(0));
    return sign(claims, Date.from(now), Date.from(exp), jti);
  }

  // Overload: WithSession（sid含める）
  public String issueAccessToken(WithSession user) {
    Instant now = Instant.now();
    Instant exp = now.plus(props.getAccessTtl());
    Map<String, Object> claims = new HashMap<>();
    claims.put("uid", user.getId().toString());
    if (user.getEmail() != null) {
      claims.put("email", user.getEmail());
    }
    claims.put("ver", Optional.ofNullable(user.getTokenVersion()).orElse(0));
    if (user.getSessionId() != null) {
      claims.put("sid", user.getSessionId().toString());
    }
    return sign(claims, Date.from(now), Date.from(exp), null);
  }

  public String issueRefreshToken(WithSession user, String jti) {
    Instant now = Instant.now();
    Instant exp = now.plus(props.getRefreshTtl());
    Map<String, Object> claims = new HashMap<>();
    claims.put("uid", user.getId().toString());
    claims.put("ver", Optional.ofNullable(user.getTokenVersion()).orElse(0));
    if (user.getSessionId() != null) {
      claims.put("sid", user.getSessionId().toString());
    }
    return sign(claims, Date.from(now), Date.from(exp), jti);
  }

  private String sign(Map<String, Object> custom, Date iat, Date exp, String jti) {
    try {
      JWTClaimsSet.Builder b =
          new JWTClaimsSet.Builder()
              .issuer(props.getIssuer())
              .audience(props.getAudience())
              .issueTime(iat)
              .expirationTime(exp)
              .jwtID(jti != null ? jti : UUID.randomUUID().toString());
      custom.forEach(b::claim);
      SignedJWT jwt =
          new SignedJWT(
              new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(keys.getKeyId()).build(), b.build());
      jwt.sign(new RSASSASigner(keys.getPrivateKey()));
      return jwt.serialize();
    } catch (Exception e) {
      throw new IllegalStateException("JWT署名に失敗", e);
    }
  }

  public ParsedJwt parseAndValidate(String token) throws ParseException {
    SignedJWT jwt = SignedJWT.parse(token);
    boolean sigValid;
    try {
      sigValid = jwt.verify(new RSASSAVerifier(keys.getPublicKey()));
    } catch (Exception e) {
      throw new ParseException("署名検証失敗", 0);
    }
    if (!sigValid) {
      throw new ParseException("署名不正", 0);
    }
    JWTClaimsSet c = jwt.getJWTClaimsSet();
    Instant now = Instant.now();
    if (c.getExpirationTime() == null || c.getExpirationTime().toInstant().isBefore(now)) {
      throw new ParseException("期限切れ", 0);
    }
    if (!props.getIssuer().equals(c.getIssuer())) {
      throw new ParseException("iss不一致", 0);
    }
    if (c.getAudience() == null
        || c.getAudience().isEmpty()
        || !c.getAudience().contains(props.getAudience())) {
      throw new ParseException("aud不一致", 0);
    }
    return new ParsedJwt(jwt, c);
  }

  public static record ParsedJwt(SignedJWT jwt, JWTClaimsSet claims) {}

  // AT/RTに必要な属性＋セッションID
  public interface WithSession {
    UUID getId();

    String getEmail();

    Integer getTokenVersion();

    UUID getSessionId();
  }
}
