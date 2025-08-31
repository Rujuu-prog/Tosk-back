package com.tosk.app.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Entity
@Table(name = "auth_session")
public class AuthSessionEntity {
  @Id
  @Column(name = "id", nullable = false, columnDefinition = "uuid")
  private UUID id; // sid

  @Column(name = "user_id", nullable = false, columnDefinition = "uuid")
  private UUID userId;

  @Column(name = "current_rt_jti", nullable = false)
  private String currentRtJti;

  @Column(name = "revoked_at")
  private OffsetDateTime revokedAt;

  @Column(name = "last_rotated_at")
  private OffsetDateTime lastRotatedAt;

  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;

  @Column(name = "ip")
  private String ip;

  @Column(name = "user_agent")
  private String userAgent;
}
