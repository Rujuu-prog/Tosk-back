package com.tosk.app.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
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
@Table(name = "users")
public class UserEntity {

  @Id
  @Column(name = "id", nullable = false, columnDefinition = "uuid")
  private UUID id;

  @Column(name = "email", nullable = false)
  private String email;

  @Column(name = "username", nullable = false)
  private String username;

  @Column(name = "display_name", nullable = false)
  private String displayName;

  @Column(name = "password_hash")
  private String passwordHash;

  @Column(name = "email_verified")
  @lombok.Builder.Default
  private Boolean emailVerified = Boolean.FALSE;

  // オプション: 全無効化用途（Flywayで追加する想定。テストではDDLで列が作られる）
  @Column(name = "token_version")
  @lombok.Builder.Default
  private Integer tokenVersion = 0;

  @Column(name = "created_at")
  private OffsetDateTime createdAt;

  @Column(name = "updated_at")
  private OffsetDateTime updatedAt;

  @PrePersist
  void prePersist() {
    if (id == null) {
      id = UUID.randomUUID();
    }
    OffsetDateTime now = OffsetDateTime.now();
    if (createdAt == null) {
      createdAt = now;
    }
    if (updatedAt == null) {
      updatedAt = now;
    }
    if (tokenVersion == null) {
      tokenVersion = 0;
    }
  }
}
