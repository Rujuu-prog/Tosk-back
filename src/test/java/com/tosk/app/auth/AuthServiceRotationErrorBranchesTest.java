package com.tosk.app.auth;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.tosk.app.user.UserEntity;
import com.tosk.app.user.UserRepository;
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
class AuthServiceRotationErrorBranchesTest {
  @Autowired AuthService authService;
  @Autowired AuthSessionRepository sessions;
  @Autowired UserRepository users;

  private UserEntity newUser() {
    UserEntity u = new UserEntity();
    u.setEmail("rot-" + UUID.randomUUID() + "@example.com");
    u.setUsername("rot" + UUID.randomUUID().toString().substring(0, 8));
    u.setDisplayName("Rot");
    u.setPasswordHash("noop");
    return users.save(u);
  }

  @Test
  void rotateOrRejectThrowsWhenSessionMissing() {
    var u = newUser();
    assertThrows(
        IllegalArgumentException.class,
        () -> authService.rotateOrReject(u.getId(), UUID.randomUUID(), "jti"));
  }

  @Test
  void rotateOrRejectThrowsWhenSessionRevoked() {
    var u = newUser();
    var s =
        sessions.save(
            AuthSessionEntity.builder()
                .id(UUID.randomUUID())
                .userId(u.getId())
                .currentRtJti("abc")
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .lastRotatedAt(OffsetDateTime.now())
                .revokedAt(OffsetDateTime.now())
                .build());
    assertThrows(
        IllegalArgumentException.class,
        () -> authService.rotateOrReject(u.getId(), s.getId(), "abc"));
  }
}
