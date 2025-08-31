package com.tosk.app.security;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {
  private String issuer = "com.tosk.app";
  private String audience = "tosk.web";
  private Duration accessTtl = Duration.ofMinutes(10);
  private Duration refreshTtl = Duration.ofDays(14);
  private String accessCookie = "AT";
  private String refreshCookie = "RT";
  private String accessCookiePath = "/";
  private String refreshCookiePath = "/auth";
  private boolean cookieSecure = true;
  private String cookieSameSite = "Strict"; // Strict/Lax/None
  private String cookieDomain; // optional
  private String publicKeyPem; // for RS256
  private String privateKeyPem; // for RS256

  public String getIssuer() {
    return issuer;
  }

  public void setIssuer(String issuer) {
    this.issuer = issuer;
  }

  public String getAudience() {
    return audience;
  }

  public void setAudience(String audience) {
    this.audience = audience;
  }

  public Duration getAccessTtl() {
    return accessTtl;
  }

  public void setAccessTtl(Duration accessTtl) {
    this.accessTtl = accessTtl;
  }

  public Duration getRefreshTtl() {
    return refreshTtl;
  }

  public void setRefreshTtl(Duration refreshTtl) {
    this.refreshTtl = refreshTtl;
  }

  public String getAccessCookie() {
    return accessCookie;
  }

  public void setAccessCookie(String accessCookie) {
    this.accessCookie = accessCookie;
  }

  public String getRefreshCookie() {
    return refreshCookie;
  }

  public void setRefreshCookie(String refreshCookie) {
    this.refreshCookie = refreshCookie;
  }

  public String getAccessCookiePath() {
    return accessCookiePath;
  }

  public void setAccessCookiePath(String accessCookiePath) {
    this.accessCookiePath = accessCookiePath;
  }

  public String getRefreshCookiePath() {
    return refreshCookiePath;
  }

  public void setRefreshCookiePath(String refreshCookiePath) {
    this.refreshCookiePath = refreshCookiePath;
  }

  public boolean isCookieSecure() {
    return cookieSecure;
  }

  public void setCookieSecure(boolean cookieSecure) {
    this.cookieSecure = cookieSecure;
  }

  public String getCookieSameSite() {
    return cookieSameSite;
  }

  public void setCookieSameSite(String cookieSameSite) {
    this.cookieSameSite = cookieSameSite;
  }

  public String getCookieDomain() {
    return cookieDomain;
  }

  public void setCookieDomain(String cookieDomain) {
    this.cookieDomain = cookieDomain;
  }

  public String getPublicKeyPem() {
    return publicKeyPem;
  }

  public void setPublicKeyPem(String publicKeyPem) {
    this.publicKeyPem = publicKeyPem;
  }

  public String getPrivateKeyPem() {
    return privateKeyPem;
  }

  public void setPrivateKeyPem(String privateKeyPem) {
    this.privateKeyPem = privateKeyPem;
  }
}
