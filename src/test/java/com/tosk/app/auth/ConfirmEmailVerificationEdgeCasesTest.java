package com.tosk.app.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:tosk;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
      "spring.datasource.driver-class-name=org.h2.Driver",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.flyway.enabled=false"
    })
class ConfirmEmailVerificationEdgeCasesTest {
  @Autowired AuthService authService;
  @Autowired EmailVerificationTokenRepository emailRepo;

  @Test
  void confirmReturnsFalseWhenExpired() {
    EmailVerificationTokenEntity t = new EmailVerificationTokenEntity();
    t.setId(UUID.randomUUID());
    t.setUserId(UUID.randomUUID());
    t.setToken(UUID.randomUUID().toString());
    t.setCreatedAt(OffsetDateTime.now().minusDays(3));
    t.setExpiresAt(OffsetDateTime.now().minusDays(1));
    emailRepo.save(t);
    boolean ok = authService.confirmEmailVerification(t.getToken());
    assertThat(ok).isFalse();
  }

  @Test
  void confirmReturnsFalseWhenAlreadyConsumed() {
    EmailVerificationTokenEntity t = new EmailVerificationTokenEntity();
    t.setId(UUID.randomUUID());
    t.setUserId(UUID.randomUUID());
    t.setToken(UUID.randomUUID().toString());
    t.setCreatedAt(OffsetDateTime.now().minusDays(1));
    t.setExpiresAt(OffsetDateTime.now().plusDays(1));
    t.setConsumedAt(OffsetDateTime.now());
    emailRepo.save(t);
    boolean ok = authService.confirmEmailVerification(t.getToken());
    assertThat(ok).isFalse();
  }
}
