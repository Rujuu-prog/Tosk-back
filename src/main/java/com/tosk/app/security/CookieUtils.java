package com.tosk.app.security;

import jakarta.servlet.http.Cookie;

public final class CookieUtils {
  private CookieUtils() {}

  public static Cookie buildCookie(
      String name,
      String value,
      int maxAgeSec,
      boolean httpOnly,
      boolean secure,
      String sameSite,
      String domain) {
    return buildCookie(name, value, maxAgeSec, httpOnly, secure, sameSite, domain, "/");
  }

  public static Cookie buildCookie(
      String name,
      String value,
      int maxAgeSec,
      boolean httpOnly,
      boolean secure,
      String sameSite,
      String domain,
      String path) {
    Cookie c = new Cookie(name, value);
    c.setPath(path == null || path.isBlank() ? "/" : path);
    c.setMaxAge(maxAgeSec);
    c.setHttpOnly(httpOnly);
    c.setSecure(secure);
    if (domain != null && !domain.isBlank()) {
      c.setDomain(domain);
    }
    // SameSite 属性は標準APIにないため、レスポンスヘッダの加工で付与（Filter/Controllerで対応）
    return c;
  }
}
