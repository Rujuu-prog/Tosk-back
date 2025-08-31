package com.tosk.app.auth.dto;

import com.tosk.app.user.UserEntity;
import java.util.UUID;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class UserResponseDTO {
  private UUID id;
  private String email;
  private String username;
  private String displayName;
  private Boolean emailVerified;

  public static UserResponseDTO from(UserEntity u) {
    return UserResponseDTO.builder()
        .id(u.getId())
        .email(u.getEmail())
        .username(u.getUsername())
        .displayName(u.getDisplayName())
        .emailVerified(u.getEmailVerified())
        .build();
  }
}
