package com.tosk.app.task.dto;

import com.tosk.app.task.TaskEntity;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class TaskResponseDTO {

  private UUID id;
  private UUID userId;
  private UUID teamId;
  private String title;
  private String description;
  private LocalDate dueDate;
  private TaskEntity.Priority priority;
  private TaskEntity.Visibility visibility;
  private Integer likeCount;
  private Integer commentCount;
  private OffsetDateTime createdAt;
  private OffsetDateTime updatedAt;

  public static TaskResponseDTO from(TaskEntity task) {
    return TaskResponseDTO.builder()
        .id(task.getId())
        .userId(task.getUserId())
        .teamId(task.getTeamId())
        .title(task.getTitle())
        .description(task.getDescription())
        .dueDate(task.getDueDate())
        .priority(task.getPriority())
        .visibility(task.getVisibility())
        .likeCount(task.getLikeCount())
        .commentCount(task.getCommentCount())
        .createdAt(task.getCreatedAt())
        .updatedAt(task.getUpdatedAt())
        .build();
  }
}
