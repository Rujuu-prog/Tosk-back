package com.tosk.app.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class LoginRequestDTO {
  @Email @NotBlank private String email;
  @NotBlank private String password;
}
