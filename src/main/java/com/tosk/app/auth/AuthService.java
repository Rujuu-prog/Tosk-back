package com.tosk.app.auth;

import static com.tosk.app.common.AuditLogger.*;

import com.tosk.app.common.ApiConstants;
import com.tosk.app.security.JwtService;
import com.tosk.app.user.UserEntity;
import com.tosk.app.user.UserRepository;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
  private final UserRepository users;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final AuthSessionRepository sessions;
  private final EmailVerificationTokenRepository emailTokens;
  private final PasswordResetTokenRepository resetTokens;
  private final com.tosk.app.common.MailService mailService;
  private final com.tosk.app.common.MailTemplateService mailTemplateService;
  private final com.tosk.app.common.AppProperties appProperties;
  private final io.micrometer.core.instrument.MeterRegistry meterRegistry;

  public AuthService(
      UserRepository users,
      PasswordEncoder passwordEncoder,
      JwtService jwtService,
      AuthSessionRepository sessions,
      EmailVerificationTokenRepository emailTokens,
      PasswordResetTokenRepository resetTokens,
      com.tosk.app.common.MailService mailService,
      com.tosk.app.common.AppProperties appProperties,
      com.tosk.app.common.MailTemplateService mailTemplateService,
      io.micrometer.core.instrument.MeterRegistry meterRegistry) {
    this.users = users;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
    this.sessions = sessions;
    this.emailTokens = emailTokens;
    this.resetTokens = resetTokens;
    this.mailService = mailService;
    this.appProperties = appProperties;
    this.mailTemplateService = mailTemplateService;
    this.meterRegistry = meterRegistry;
  }

  @Transactional
  public UserEntity signup(String email, String username, String displayName, String rawPassword) {
    users
        .findByEmailIgnoreCase(email)
        .ifPresent(
            u -> {
              throw new IllegalArgumentException("メールアドレスは既に使用されています");
            });
    users
        .findByUsernameIgnoreCase(username)
        .ifPresent(
            u -> {
              throw new IllegalArgumentException("ユーザー名は既に使用されています");
            });
    UserEntity u = new UserEntity();
    u.setEmail(email.trim());
    u.setUsername(username.trim());
    u.setDisplayName(displayName.trim());
    u.setPasswordHash(passwordEncoder.encode(rawPassword));
    UserEntity saved = users.save(u);
    info(ApiConstants.EVT_SIGNUP_SUCCESS, Map.of("uid", saved.getId().toString()));
    return saved;
  }

  @Transactional(readOnly = true)
  public UserEntity login(String email, String rawPassword) {
    UserEntity u =
        users.findByEmailIgnoreCase(email).orElseThrow(() -> new IllegalArgumentException("認証失敗"));
    if (u.getPasswordHash() == null || !passwordEncoder.matches(rawPassword, u.getPasswordHash())) {
      warn(ApiConstants.EVT_LOGIN_FAILED, Map.of("sub_hash", hash(email)));
      throw new com.tosk.app.common.UnauthorizedException("認証失敗");
    }
    info(ApiConstants.EVT_LOGIN_SUCCESS, Map.of("uid", u.getId().toString()));
    // metric
    if (meterRegistry != null) {
      meterRegistry.counter("tosk.auth.login.success_total").increment();
    }
    return u;
  }

  public String issueAccessToken(UserEntity u) {
    return jwtService.issueAccessToken(u);
  }

  public String issueRefreshToken(UserEntity u, String jti) {
    return jwtService.issueRefreshToken(u, jti);
  }

  public String issueAccessToken(JwtService.WithSession u) {
    return jwtService.issueAccessToken(u);
  }

  public String issueRefreshToken(JwtService.WithSession u, String jti) {
    return jwtService.issueRefreshToken(u, jti);
  }

  @Transactional
  public AuthSessionEntity createSession(
      UserEntity u, String ip, String userAgent, String initialJti) {
    AuthSessionEntity s =
        AuthSessionEntity.builder()
            .id(UUID.randomUUID())
            .userId(u.getId())
            .currentRtJti(initialJti)
            .createdAt(OffsetDateTime.now())
            .updatedAt(OffsetDateTime.now())
            .lastRotatedAt(OffsetDateTime.now())
            .ip(ip)
            .userAgent(userAgent)
            .build();
    return sessions.save(s);
  }

  @Transactional
  public RotationResult rotateOrReject(UUID uid, UUID sid, String presentedJti) {
    AuthSessionEntity s =
        sessions
            .findByIdAndUserId(sid, uid)
            .orElseThrow(() -> new IllegalArgumentException("セッションが存在しません"));
    if (s.getRevokedAt() != null) {
      throw new IllegalArgumentException("セッションは失効済みです");
    }
    if (!s.getCurrentRtJti().equals(presentedJti)) {
      // 再利用検知: 即時失効
      s.setRevokedAt(OffsetDateTime.now());
      s.setUpdatedAt(OffsetDateTime.now());
      sessions.save(s);
      warn(
          ApiConstants.EVT_RT_REUSE,
          Map.of(
              "uid", uid.toString(),
              "sid", sid.toString(),
              "jti", presentedJti));
      if (meterRegistry != null) {
        meterRegistry.counter("tosk.auth.rt_reuse_detected_total").increment();
      }
      throw new com.tosk.app.common.TokenReuseDetectedException("リフレッシュトークン再利用を検知しました");
    }
    String newJti = UUID.randomUUID().toString();
    s.setCurrentRtJti(newJti);
    s.setLastRotatedAt(OffsetDateTime.now());
    s.setUpdatedAt(OffsetDateTime.now());
    sessions.save(s);
    return new RotationResult(s, newJti);
  }

  @Transactional
  public void revokeSession(UUID uid, UUID sid) {
    sessions
        .findByIdAndUserId(sid, uid)
        .ifPresent(
            s -> {
              s.setRevokedAt(OffsetDateTime.now());
              s.setUpdatedAt(OffsetDateTime.now());
              sessions.save(s);
            });
    info(
        ApiConstants.EVT_LOGOUT,
        Map.of(
            "uid", uid.toString(),
            "sid", sid.toString()));
    if (meterRegistry != null) {
      meterRegistry.counter("tosk.auth.logout_total").increment();
    }
  }

  public record RotationResult(AuthSessionEntity session, String newJti) {}

  // ---- Email Verification ----
  @Transactional
  public void startEmailVerification(UUID userId) {
    EmailVerificationTokenEntity t = new EmailVerificationTokenEntity();
    t.setId(UUID.randomUUID());
    t.setUserId(userId);
    t.setToken(UUID.randomUUID().toString());
    t.setCreatedAt(OffsetDateTime.now());
    t.setExpiresAt(OffsetDateTime.now().plusDays(2));
    emailTokens.save(t);
    info(ApiConstants.EVT_VERIFY_START, Map.of("uid", userId.toString()));
    if (meterRegistry != null) {
      meterRegistry.counter("tosk.auth.verify.start_total").increment();
    }
    users
        .findById(userId)
        .ifPresent(
            u -> {
              String base = appProperties.getFrontendBaseUrl();
              String link = base + "/verify?token=" + t.getToken();
              String subject = appProperties.getMail().getSubjectPrefix() + "Verify your email";
              String locale =
                  appProperties.getMail().getLocale() == null
                      ? "ja"
                      : appProperties.getMail().getLocale();
              String text =
                  mailTemplateService.renderLocalized(
                      "mail/verification",
                      locale,
                      "txt",
                      java.util.Map.of(
                          "subject", subject,
                          "link", link));
              String html = null;
              try {
                html =
                    mailTemplateService.renderLocalized(
                        "mail/verification",
                        locale,
                        "html",
                        java.util.Map.of(
                            "subject", subject,
                            "link", link));
              } catch (Exception ignore) {
              }
              mailService.send(
                  appProperties.getMail().getFrom(), u.getEmail(), subject, text, html);
            });
  }

  @Transactional
  public boolean confirmEmailVerification(String token) {
    EmailVerificationTokenEntity t = emailTokens.findByToken(token).orElse(null);
    if (t == null || t.getConsumedAt() != null || t.getExpiresAt().isBefore(OffsetDateTime.now())) {
      return false;
    }
    users
        .findById(t.getUserId())
        .ifPresent(
            u -> {
              u.setEmailVerified(true);
              users.save(u);
            });
    t.setConsumedAt(OffsetDateTime.now());
    emailTokens.save(t);
    info(ApiConstants.EVT_VERIFY_CONFIRMED, Map.of("uid", t.getUserId().toString()));
    if (meterRegistry != null) {
      meterRegistry.counter("tosk.auth.verify.confirmed_total").increment();
    }
    return true;
  }

  // ---- Password Reset ----
  @Transactional
  public void startPasswordReset(String email) {
    users
        .findByEmailIgnoreCase(email)
        .ifPresent(
            u -> {
              PasswordResetTokenEntity t = new PasswordResetTokenEntity();
              t.setId(UUID.randomUUID());
              t.setUserId(u.getId());
              t.setToken(UUID.randomUUID().toString());
              t.setCreatedAt(OffsetDateTime.now());
              t.setExpiresAt(OffsetDateTime.now().plusHours(2));
              resetTokens.save(t);
              info(ApiConstants.EVT_PW_RESET_START, Map.of("uid", u.getId().toString()));
              if (meterRegistry != null) {
                meterRegistry.counter("tosk.auth.pw_reset.start_total").increment();
              }
              String base = appProperties.getFrontendBaseUrl();
              String link = base + "/reset?token=" + t.getToken();
              String subject = appProperties.getMail().getSubjectPrefix() + "Password reset";
              String locale =
                  appProperties.getMail().getLocale() == null
                      ? "ja"
                      : appProperties.getMail().getLocale();
              String text =
                  mailTemplateService.renderLocalized(
                      "mail/password_reset",
                      locale,
                      "txt",
                      java.util.Map.of(
                          "subject", subject,
                          "link", link));
              String html = null;
              try {
                html =
                    mailTemplateService.renderLocalized(
                        "mail/password_reset",
                        locale,
                        "html",
                        java.util.Map.of(
                            "subject", subject,
                            "link", link));
              } catch (Exception ignore) {
              }
              mailService.send(
                  appProperties.getMail().getFrom(), u.getEmail(), subject, text, html);
            });
  }

  @Transactional
  public boolean confirmPasswordReset(String token, String newPassword) {
    PasswordResetTokenEntity t = resetTokens.findByToken(token).orElse(null);
    if (t == null || t.getConsumedAt() != null || t.getExpiresAt().isBefore(OffsetDateTime.now())) {
      return false;
    }
    users
        .findById(t.getUserId())
        .ifPresent(
            u -> {
              u.setPasswordHash(passwordEncoder.encode(newPassword));
              // 任意: 全失効
              u.setTokenVersion((u.getTokenVersion() == null ? 0 : u.getTokenVersion()) + 1);
              users.save(u);
            });
    t.setConsumedAt(OffsetDateTime.now());
    resetTokens.save(t);
    info(ApiConstants.EVT_PW_RESET_CONFIRMED, Map.of("uid", t.getUserId().toString()));
    if (meterRegistry != null) {
      meterRegistry.counter("tosk.auth.pw_reset.confirmed_total").increment();
    }
    return true;
  }

  @Transactional(readOnly = true)
  public java.util.List<AuthSessionEntity> listSessions(UUID uid) {
    return sessions.findAllByUserIdOrderByCreatedAtDesc(uid);
  }

  @Transactional(readOnly = true)
  public java.util.Optional<AuthSessionEntity> getSession(UUID uid, UUID sid) {
    return sessions.findByIdAndUserId(sid, uid);
  }

  @Transactional
  public void revokeAll(UUID uid, java.util.Optional<UUID> keepSid) {
    var list = sessions.findAllByUserIdOrderByCreatedAtDesc(uid);
    for (var s : list) {
      if (keepSid.isPresent() && keepSid.get().equals(s.getId())) {
        continue;
      }
      if (s.getRevokedAt() == null) {
        s.setRevokedAt(OffsetDateTime.now());
        s.setUpdatedAt(OffsetDateTime.now());
        sessions.save(s);
      }
    }
    info(ApiConstants.EVT_SESSIONS_REVOKE_ALL, Map.of("uid", uid.toString()));
    if (meterRegistry != null) {
      meterRegistry.counter("tosk.auth.sessions.revoke_all_total").increment();
    }
  }
}
