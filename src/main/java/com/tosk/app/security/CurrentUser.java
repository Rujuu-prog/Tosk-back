package com.tosk.app.security;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CurrentUser {
  private UUID id;
  private String username;
  private String email;
}
