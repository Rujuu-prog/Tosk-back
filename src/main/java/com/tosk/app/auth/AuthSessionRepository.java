package com.tosk.app.auth;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuthSessionRepository extends JpaRepository<AuthSessionEntity, UUID> {
  Optional<AuthSessionEntity> findByIdAndUserId(UUID id, UUID userId);

  java.util.List<AuthSessionEntity> findAllByUserIdOrderByCreatedAtDesc(UUID userId);
}
