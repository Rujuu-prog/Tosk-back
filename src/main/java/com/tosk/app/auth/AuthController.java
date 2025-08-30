package com.tosk.app.auth;

import com.nimbusds.jwt.JWTClaimsSet;
import com.tosk.app.auth.dto.LoginRequestDTO;
import com.tosk.app.auth.dto.SignupRequestDTO;
import com.tosk.app.auth.dto.UserResponseDTO;
import com.tosk.app.security.CookieUtils;
import com.tosk.app.security.JwtProperties;
import com.tosk.app.security.JwtService;
import com.tosk.app.security.LoginRateLimiter;
import com.tosk.app.user.UserEntity;
import com.tosk.app.user.UserRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.text.ParseException;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.DeleteMapping;

@RestController
@RequestMapping("/api/auth")
@Validated
public class AuthController {
  private static final Logger LOGGER = LoggerFactory.getLogger(AuthController.class);
  private static final String CLAIM_SID = "sid";
  private static final String RESPONSE_STATUS = "status";
  private static final String SET_COOKIE_HEADER = "Set-Cookie";
  private final AuthService authService;
  private final JwtService jwtService;
  private final JwtProperties props;
  private final UserRepository userRepository;
  private final LoginRateLimiter loginRateLimiter;

  public AuthController(
      final AuthService authService,
      final JwtService jwtService,
      final JwtProperties props,
      final UserRepository userRepository,
      final LoginRateLimiter loginRateLimiter) {
    this.authService = authService;
    this.jwtService = jwtService;
    this.props = props;
    this.userRepository = userRepository;
    this.loginRateLimiter = loginRateLimiter;
  }

  @PostMapping("/signup")
  public ResponseEntity<?> signup(
      @Valid @RequestBody SignupRequestDTO req,
      final HttpServletRequest httpReq,
      final HttpServletResponse res) {
    UserEntity u =
        authService.signup(
            req.getEmail(), req.getUsername(), req.getDisplayName(), req.getPassword());
    String initialJti = UUID.randomUUID().toString();
    AuthSessionEntity s =
        authService.createSession(u, clientIp(httpReq), userAgent(httpReq), initialJti);
    UserWithSession uw = new UserWithSession(u, s.getId());
    setAuthCookies(res, uw, initialJti);
    return ResponseEntity.status(HttpStatus.CREATED).body(UserResponseDTO.from(u));
  }

  @PostMapping("/login")
  public ResponseEntity<?> login(
      @Valid @RequestBody LoginRequestDTO req,
      final HttpServletRequest httpReq,
      final HttpServletResponse res) {
    // レート制限
    String rateKey =
        (req.getEmail() == null ? "" : req.getEmail().toLowerCase(Locale.ENGLISH))
            + "|"
            + clientIp(httpReq);
    loginRateLimiter.checkAndIncrement(rateKey);
    UserEntity u = authService.login(req.getEmail(), req.getPassword());
    String initialJti = UUID.randomUUID().toString();
    AuthSessionEntity s =
        authService.createSession(u, clientIp(httpReq), userAgent(httpReq), initialJti);
    UserWithSession uw = new UserWithSession(u, s.getId());
    setAuthCookies(res, uw, initialJti);
    return ResponseEntity.ok(UserResponseDTO.from(u));
  }

  @PostMapping("/refresh")
  public ResponseEntity<?> refresh(HttpServletRequest request, HttpServletResponse response)
      throws ParseException {
    String rt = extractCookie(request, props.getRefreshCookie());
    if (rt == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(
              Map.of(
                  "errorCode",
                  com.tosk.app.common.ApiConstants.ERR_TOKEN_MISSING,
                  "message",
                  "refresh token missing"));
    }
    JwtService.ParsedJwt pj = jwtService.parseAndValidate(rt);
    JWTClaimsSet c = pj.claims();
    UUID uid = UUID.fromString((String) c.getClaim("uid"));
    UUID sid = UUID.fromString((String) c.getClaim(CLAIM_SID));
    String jti = pj.jwt().getJWTClaimsSet().getJWTID();
    try {
      AuthService.RotationResult rr = authService.rotateOrReject(uid, sid, jti);
      // 新しいAT/RTを発行
      UserEntity u = new UserEntity();
      u.setId(uid);
      Object verObj = c.getClaim("ver");
      Integer ver =
          verObj instanceof Number
              ? ((Number) verObj).intValue()
              : verObj != null ? Integer.parseInt(verObj.toString()) : 0;
      u.setTokenVersion(ver);
      UserWithSession uw = new UserWithSession(u, rr.session().getId());
      setAuthCookies(response, uw, rr.newJti());
      return ResponseEntity.ok(Map.of(RESPONSE_STATUS, "rotated"));
    } catch (com.tosk.app.common.TokenReuseDetectedException ex) {
      clearCookies(response);
      return ResponseEntity.status(HttpStatus.CONFLICT)
          .body(
              Map.of(
                  "errorCode",
                  com.tosk.app.common.ApiConstants.ERR_TOKEN_REUSE_DETECTED,
                  "message",
                  ex.getMessage()));
    } catch (IllegalArgumentException ex) {
      clearCookies(response);
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(
              Map.of(
                  "errorCode",
                  com.tosk.app.common.ApiConstants.ERR_UNAUTHORIZED,
                  "message",
                  ex.getMessage()));
    }
  }

  @PostMapping("/logout")
  public ResponseEntity<?> logout(HttpServletRequest req, HttpServletResponse res)
      throws ParseException {
    String rt = extractCookie(req, props.getRefreshCookie());
    if (rt != null) {
      try {
        JwtService.ParsedJwt pj = jwtService.parseAndValidate(rt);
        JWTClaimsSet c = pj.claims();
        UUID uid = UUID.fromString((String) c.getClaim("uid"));
        UUID sid = UUID.fromString((String) c.getClaim(CLAIM_SID));
        authService.revokeSession(uid, sid);
      } catch (Exception ex) {
        // 不正RTならそのままクッキー破棄
        LOGGER.debug("Invalid refresh token during logout: {}", ex.getMessage());
      }
    }
    clearCookies(res);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/me")
  public ResponseEntity<?> me() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || auth.getPrincipal() == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    UUID uid = (UUID) auth.getPrincipal();
    return userRepository
        .findById(uid)
        .map(UserResponseDTO::from)
        .<ResponseEntity<?>>map(ResponseEntity::ok)
        .orElseGet(
            () ->
                ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "user not found")));
  }

  @GetMapping("/sessions")
  public ResponseEntity<?> mySessions() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || auth.getPrincipal() == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    UUID uid = (UUID) auth.getPrincipal();
    var list =
        authService.listSessions(uid).stream()
            .map(com.tosk.app.auth.dto.AuthSessionDTO::from)
            .toList();
    return ResponseEntity.ok(list);
  }

  @org.springframework.web.bind.annotation.DeleteMapping("/sessions/{sid}")
  public ResponseEntity<?> revokeSession(
      @org.springframework.web.bind.annotation.PathVariable("sid") UUID sid) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || auth.getPrincipal() == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    UUID uid = (UUID) auth.getPrincipal();
    authService.revokeSession(uid, sid);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/sessions/{sid}")
  public ResponseEntity<?> sessionDetail(@PathVariable("sid") UUID sid) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || auth.getPrincipal() == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    UUID uid = (UUID) auth.getPrincipal();
    return authService
        .getSession(uid, sid)
        .<ResponseEntity<?>>map(
            s -> ResponseEntity.ok(com.tosk.app.auth.dto.AuthSessionDTO.from(s)))
        .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
  }

  @DeleteMapping("/sessions")
  public ResponseEntity<?> revokeAllSessions(
      @org.springframework.web.bind.annotation.RequestParam(value = "keepCurrent", required = false)
          Boolean keepCurrent,
      HttpServletRequest req)
      throws java.text.ParseException {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || auth.getPrincipal() == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    UUID uid = (UUID) auth.getPrincipal();
    java.util.Optional<UUID> keep = java.util.Optional.empty();
    if (Boolean.TRUE.equals(keepCurrent)) {
      String at = extractCookie(req, props.getAccessCookie());
      if (at != null) {
        var pj = jwtService.parseAndValidate(at);
        String sidStr = (String) pj.claims().getClaim(CLAIM_SID);
        if (sidStr != null) {
          keep = java.util.Optional.of(java.util.UUID.fromString(sidStr));
        }
      }
    }
    authService.revokeAll(uid, keep);
    return ResponseEntity.noContent().build();
  }

  private void setAuthCookies(HttpServletResponse res, UserWithSession u, String rtJti) {
    String at = authService.issueAccessToken(u);
    String rt = authService.issueRefreshToken(u, rtJti);
    Cookie c1 =
        CookieUtils.buildCookie(
            props.getAccessCookie(),
            at,
            (int) props.getAccessTtl().toSeconds(),
            true,
            props.isCookieSecure(),
            props.getCookieSameSite(),
            props.getCookieDomain(),
            props.getAccessCookiePath());
    Cookie c2 =
        CookieUtils.buildCookie(
            props.getRefreshCookie(),
            rt,
            (int) props.getRefreshTtl().toSeconds(),
            true,
            props.isCookieSecure(),
            props.getCookieSameSite(),
            props.getCookieDomain(),
            props.getRefreshCookiePath());
    // SameSite を強制付与
    res.addHeader(SET_COOKIE_HEADER, buildSetCookie(c1, props.getCookieSameSite()));
    res.addHeader(SET_COOKIE_HEADER, buildSetCookie(c2, props.getCookieSameSite()));
  }

  private void clearCookies(HttpServletResponse res) {
    Cookie c1 =
        CookieUtils.buildCookie(
            props.getAccessCookie(),
            "",
            0,
            true,
            props.isCookieSecure(),
            props.getCookieSameSite(),
            props.getCookieDomain(),
            props.getAccessCookiePath());
    Cookie c2 =
        CookieUtils.buildCookie(
            props.getRefreshCookie(),
            "",
            0,
            true,
            props.isCookieSecure(),
            props.getCookieSameSite(),
            props.getCookieDomain(),
            props.getRefreshCookiePath());
    res.addHeader(SET_COOKIE_HEADER, buildSetCookie(c1, props.getCookieSameSite()));
    res.addHeader(SET_COOKIE_HEADER, buildSetCookie(c2, props.getCookieSameSite()));
  }

  static String buildSetCookie(Cookie c, String sameSite) {
    StringBuilder sb = new StringBuilder();
    sb.append(c.getName()).append("=").append(c.getValue() == null ? "" : c.getValue());
    sb.append("; Path=").append(c.getPath());
    if (c.getDomain() != null) {
      sb.append("; Domain=").append(c.getDomain());
    }
    sb.append("; Max-Age=").append(c.getMaxAge());
    if (c.getSecure()) {
      sb.append("; Secure");
    }
    if (c.isHttpOnly()) {
      sb.append("; HttpOnly");
    }
    String ss = (sameSite == null || sameSite.isBlank()) ? "Strict" : sameSite;
    sb.append("; SameSite=").append(ss);
    return sb.toString();
  }

  private static String extractCookie(HttpServletRequest req, String name) {
    if (req.getCookies() == null) {
      return null;
    }
    for (Cookie c : req.getCookies()) {
      if (name.equals(c.getName())) {
        return c.getValue();
      }
    }
    return null;
  }

  // AT/RTの作成時にsidを含めるためのアダプタ
  record UserWithSession(UserEntity delegate, UUID sessionId) implements JwtService.WithSession {
    @Override
    public UUID getSessionId() {
      return sessionId;
    }

    @Override
    public UUID getId() {
      return delegate.getId();
    }

    @Override
    public String getEmail() {
      return delegate.getEmail();
    }

    @Override
    public Integer getTokenVersion() {
      return delegate.getTokenVersion();
    }
  }

  private static String clientIp(HttpServletRequest req) {
    String xff = req.getHeader("X-Forwarded-For");
    if (xff != null && !xff.isBlank()) {
      return xff.split(",")[0].trim();
    }
    return req.getRemoteAddr();
  }

  private static String userAgent(HttpServletRequest req) {
    String ua = req.getHeader("User-Agent");
    return ua != null ? ua : "";
  }

  // ---- Email verification / Password reset ----
  @PostMapping("/verification/start")
  public ResponseEntity<?> startVerification() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || auth.getPrincipal() == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    UUID uid = (UUID) auth.getPrincipal();
    authService.startEmailVerification(uid);
    return ResponseEntity.accepted().body(Map.of(RESPONSE_STATUS, "verification_started"));
  }

  @PostMapping("/verification/confirm")
  public ResponseEntity<?> confirmVerification(@RequestParam("token") String token) {
    boolean ok = authService.confirmEmailVerification(token);
    return ok
        ? ResponseEntity.ok(Map.of(RESPONSE_STATUS, "verified"))
        : ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(Map.of("error", "invalid_or_expired_token"));
  }

  @PostMapping("/password/reset/start")
  public ResponseEntity<?> startPasswordReset(@RequestParam("email") String email) {
    // 常に202で応答（ユーザ存在の可否を秘匿）
    authService.startPasswordReset(email);
    return ResponseEntity.accepted().body(Map.of(RESPONSE_STATUS, "reset_started"));
  }

  @PostMapping("/password/reset/confirm")
  public ResponseEntity<?> confirmPasswordReset(
      @RequestParam("token") String token, @RequestParam("password") String password) {
    boolean ok = authService.confirmPasswordReset(token, password);
    return ok
        ? ResponseEntity.ok(Map.of(RESPONSE_STATUS, "password_reset"))
        : ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(Map.of("error", "invalid_or_expired_token"));
  }
}
