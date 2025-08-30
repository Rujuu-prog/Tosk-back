package com.tosk.app.security;

import com.nimbusds.jwt.JWTClaimsSet;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class JwtAuthenticationFilter extends OncePerRequestFilter {
  private final JwtService jwtService;
  private final JwtProperties props;
  private final com.tosk.app.user.UserRepository userRepository;
  private final com.tosk.app.auth.AuthSessionRepository sessionRepository;

  public JwtAuthenticationFilter(
      JwtService jwtService,
      JwtProperties props,
      com.tosk.app.user.UserRepository userRepository,
      com.tosk.app.auth.AuthSessionRepository sessionRepository) {
    this.jwtService = jwtService;
    this.props = props;
    this.userRepository = userRepository;
    this.sessionRepository = sessionRepository;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    try {
      String at = extractCookie(request, props.getAccessCookie()).orElse(null);
      if (at != null) {
        JwtService.ParsedJwt pj = jwtService.parseAndValidate(at);
        JWTClaimsSet c = pj.claims();
        String uidStr = (String) c.getClaim("uid");
        UUID uid = uidStr != null ? UUID.fromString(uidStr) : null;
        // Additional checks: ver and sid must match DB state
        Number verNum = (Number) c.getClaim("ver");
        String sidStr = (String) c.getClaim("sid");
        if (uid == null || verNum == null || sidStr == null) {
          writeUnauthorized(response, "missing claims");
          return;
        }
        int ver = verNum.intValue();
        var user = userRepository.findById(uid).orElse(null);
        if (user == null
            || user.getTokenVersion() == null
            || user.getTokenVersion().intValue() != ver) {
          writeUnauthorized(response, "token version mismatch");
          return;
        }
        UUID sid = UUID.fromString(sidStr);
        var sess = sessionRepository.findByIdAndUserId(sid, uid).orElse(null);
        if (sess == null || sess.getRevokedAt() != null) {
          writeUnauthorized(response, "session revoked or missing");
          return;
        }
        Authentication auth =
            new UsernamePasswordAuthenticationToken(
                uid, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(auth);
      }
    } catch (ParseException e) {
      writeUnauthorized(response, "invalid token");
      return;
    }
    filterChain.doFilter(request, response);
  }

  private static Optional<String> extractCookie(HttpServletRequest req, String name) {
    Cookie[] cookies = req.getCookies();
    if (cookies == null) {
      return Optional.empty();
    }
    return Arrays.stream(cookies)
        .filter(c -> name.equals(c.getName()))
        .map(Cookie::getValue)
        .findFirst();
  }

  private static void writeUnauthorized(HttpServletResponse res, String msg) throws IOException {
    res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    res.setContentType("application/json");
    String body =
        String.format(
            "{\"errorCode\":\"%s\",\"message\":\"%s\"}",
            com.tosk.app.common.ApiConstants.ERR_TOKEN_INVALID, msg.replace("\"", "'"));
    res.getOutputStream().write(body.getBytes(java.nio.charset.StandardCharsets.UTF_8));
  }
}
