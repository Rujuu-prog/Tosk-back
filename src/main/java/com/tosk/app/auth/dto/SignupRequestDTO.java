package com.tosk.app.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class SignupRequestDTO {
  @Email @NotBlank private String email;

  @NotBlank
  @Size(min = 3, max = 30)
  private String username;

  @NotBlank
  @Size(min = 1, max = 50)
  private String displayName;

  @NotBlank
  @Size(min = 8, max = 128)
  private String password;
}
