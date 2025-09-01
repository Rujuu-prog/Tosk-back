package com.tosk.app.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import lombok.Data;

@Data
public class CreateCommentRequestDTO {

  @NotBlank(message = "Content is required")
  @Size(min = 1, max = 5000, message = "Content must be between 1 and 5000 characters")
  private String content;

  private UUID parentCommentId;
}
