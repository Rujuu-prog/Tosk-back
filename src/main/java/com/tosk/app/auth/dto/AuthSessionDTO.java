package com.tosk.app.auth.dto;

import com.tosk.app.auth.AuthSessionEntity;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class AuthSessionDTO {
  private UUID id;
  private OffsetDateTime createdAt;
  private OffsetDateTime lastRotatedAt;
  private OffsetDateTime revokedAt;
  private String ip;
  private String userAgent;

  public static AuthSessionDTO from(AuthSessionEntity s) {
    return AuthSessionDTO.builder()
        .id(s.getId())
        .createdAt(s.getCreatedAt())
        .lastRotatedAt(s.getLastRotatedAt())
        .revokedAt(s.getRevokedAt())
        .ip(s.getIp())
        .userAgent(s.getUserAgent())
        .build();
  }
}
