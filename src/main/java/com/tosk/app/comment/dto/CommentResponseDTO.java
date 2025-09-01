package com.tosk.app.comment.dto;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CommentResponseDTO {

  private UUID id;
  private UUID taskId;
  private UUID userId;
  private UUID parentCommentId;
  private String content;
  private Integer likeCount;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  // Author information for display
  private String authorUsername;
  private String authorDisplayName;
  private String authorAvatarUrl;
}
