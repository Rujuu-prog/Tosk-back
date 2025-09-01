package com.tosk.app.like.dto;

import com.tosk.app.like.LikeEntity;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LikeResponseDTO {

  private UUID id;
  private UUID userId;
  private LikeEntity.TargetType targetType;
  private UUID targetId;
  private LocalDateTime createdAt;
}
