package com.tosk.app.security;

import com.nimbusds.jose.jwk.JWKSet;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class JwksController {
  private final RsaKeyProvider keyProvider;

  public JwksController(RsaKeyProvider keyProvider) {
    this.keyProvider = keyProvider;
  }

  @GetMapping(value = "/.well-known/jwks.json", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Map<String, Object>> getJwks(
      @org.springframework.web.bind.annotation.RequestHeader(
              value = "If-None-Match",
              required = false)
          String inm) {
    JWKSet set = keyProvider.getPublicJwkSet();
    Map<String, Object> body = set.toJSONObject();
    String etag = computeEtag(body);
    if (etag != null && etag.equals(inm)) {
      return ResponseEntity.status(304)
          .cacheControl(
              org.springframework.http.CacheControl.maxAge(java.time.Duration.ofSeconds(300))
                  .cachePublic())
          .eTag(etag)
          .build();
    }
    return ResponseEntity.ok()
        .cacheControl(
            org.springframework.http.CacheControl.maxAge(java.time.Duration.ofSeconds(300))
                .cachePublic())
        .eTag(etag)
        .body(body);
  }

  private static String computeEtag(Map<String, Object> body) {
    try {
      String json = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(body);
      java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
      byte[] d = md.digest(json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
      StringBuilder sb = new StringBuilder();
      for (byte b : d) {
        sb.append(String.format("%02x", b));
      }
      return '"' + sb.toString() + '"'; // quoted ETag
    } catch (Exception e) {
      return null;
    }
  }
}
