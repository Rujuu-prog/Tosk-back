package com.tosk.app.auth;

import static org.assertj.core.api.Assertions.assertThatCode;

import com.tosk.app.common.InMemoryMailService;
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
class AuthServiceMailAbsentUserTest {
  @Autowired AuthService authService;
  @Autowired InMemoryMailService mail;

  @Test
  void startVerificationWithUnknownUserDoesNotThrow() {
    mail.clear();
    assertThatCode(() -> authService.startEmailVerification(UUID.randomUUID()))
        .doesNotThrowAnyException();
    // no assertion on mailbox; just covering branch where user not present
  }

  @Test
  void startPasswordResetWithUnknownEmailDoesNotThrow() {
    mail.clear();
    assertThatCode(() -> authService.startPasswordReset("nope@example.com"))
        .doesNotThrowAnyException();
  }
}
