package com.tosk.app.user;

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
class UserEntityPrePersistTest {
  @Autowired UserRepository repo;

  @Test
  void prePersistKeepsExistingTimestampsAndTokenVersion() {
    UserEntity u = new UserEntity();
    u.setId(UUID.randomUUID());
    u.setEmail("pre@ex.com");
    u.setUsername("prex");
    u.setDisplayName("Pre X");
    u.setPasswordHash("x");
    OffsetDateTime ts = OffsetDateTime.now().minusDays(1);
    u.setCreatedAt(ts);
    u.setUpdatedAt(ts);
    u.setTokenVersion(5);

    UserEntity saved = repo.save(u);
    assertThat(saved.getCreatedAt()).isEqualTo(ts);
    assertThat(saved.getUpdatedAt()).isEqualTo(ts);
    assertThat(saved.getTokenVersion()).isEqualTo(5);
  }
}
